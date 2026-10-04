# DB 스키마 변경 관리 (수동 SQL)

prod/staging은 `ddl-auto: none`이라 엔티티를 바꿔도 스키마가 자동으로 바뀌지 않는다.
마이그레이션 도구(Flyway 등)는 쓰지 않고, **변경 SQL을 이 폴더에 남기고 배포 전에 사람이 직접 실행**한다.

## 폴더 구조

```
db/
├── schema/
│   └── baseline-2026-10-03.sql   # 기준선: 이 날짜의 prod 스키마 (구조만, 데이터 없음)
└── migration/
    ├── README.md                  # 이 문서
    └── V{번호}__{설명}.sql        # 변경 SQL (번호 순서대로 실행)
```

## 규칙

1. **파일 이름**: `V{3자리 번호}__{이슈번호}_{설명}.sql` — 예: `V001__703_host_contact.sql`
   - 번호는 이 폴더 기준으로 1씩 증가. 머지 순서가 바뀌면 번호를 다시 맞춘다.
2. **하나의 PR = 하나의 파일** (여러 테이블이어도 한 파일). 엔티티 변경과 같은 PR에 넣는다.
3. **v1이 깨지지 않는 변경만** 한다.
   - OK: 테이블 추가, nullable 컬럼 추가, 기본값 있는 NOT NULL 컬럼 추가, 인덱스 추가
   - 금지(v1 종료 전): 컬럼 삭제·이름 변경·타입 축소, NOT NULL 기본값 없이 추가
4. **데이터 이관**이 있으면 같은 파일 아래쪽에 `-- [DATA]` 주석으로 분리하고, 재실행해도 안전하게(멱등) 쓴다. `[DATA]`는 앱 배포 후 실행한다.
   - 파일 맨 위(헤더 다음)에 `SET NAMES utf8mb4;`, 큰 테이블 컬럼 추가는 `ALGORITHM=INSTANT` 를 명시한다
5. 파일 맨 위에 헤더를 쓴다.

```sql
-- V001__703_host_contact.sql
-- 이슈: Gosrock/DuDoong-Backend#703
-- 목적: 호스트 연락처 N개
-- v1 영향: 없음 (테이블 추가)
-- 실행 기록:
--   staging: YYYY-MM-DD (실행자)
--   prod:    YYYY-MM-DD (실행자)
```

## 배포 절차

순서: **DDL → 앱 배포 → `[DATA]`**

1. PR 머지 전: 로컬(`ddl-auto: none`)에서 baseline + 이전 V 파일 + 이번 SQL을 실행하고 앱이 기동·동작하는지 확인. `[DATA]`는 두 번 실행해 멱등 확인
2. staging: DDL(`-- [DATA]` 위) 실행 → 앱 배포 → `[DATA]` 실행 → 확인
3. prod: 같은 순서. DDL은 컬럼/테이블 추가만이라 먼저 넣어도 기존 v1 앱은 영향 없고, 새 앱은 새 컬럼이 있어야 동작하므로 DDL이 앱보다 먼저다.
   `[DATA]`는 배포 중 옛 앱이 쓴 값까지 반영하도록 앱 배포 **후** 실행한다
4. 한글 데이터가 들어가면 실행 후 인코딩 확인: `SELECT HEX(title) FROM tbl_event_section WHERE sort_order = 0 LIMIT 1;` → `EAB3B5EC97B020EC868CEAB09C`('공연 소개'). 다르면 클라이언트 문자셋(`SET NAMES utf8mb4`) 확인
5. 파일 헤더의 실행 기록을 채워 커밋

## 기준선 갱신

`schema/baseline-*.sql`은 구조만 덤프한 것(`mysqldump --no-data`)이며 `AUTO_INCREMENT` 값은 제거했다.
데이터·계정 정보는 절대 넣지 않는다.

### 기준선에서 확인된 참고 사항 (2026-10-03)
- `tbl_host.name`은 `varchar(15)`, `tbl_event.name`은 `varchar(30)`
- `tbl_ticket_item_item_option_groups`는 **사용 중**: `TicketItem.itemOptionGroups`가 `mappedBy`/`@JoinColumn` 없는 단방향 `@OneToMany`라 Hibernate가 조인 테이블로 매핑함. 지우면 안 됨
- `tbl_host_host_users`는 현재 `Host.hostUsers`가 `mappedBy="host"`라 쓰이지 않는 과거 조인 테이블로 보임 (정리는 v1 종료 후 검토). 2026-10-04 prod 0행 확인
