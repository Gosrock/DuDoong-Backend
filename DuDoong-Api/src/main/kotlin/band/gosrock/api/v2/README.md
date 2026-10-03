# `/api/v2` 패키지 규칙

v1(`/api/v1/**`)은 운영 중이므로 동작을 바꾸지 않는다. v2는 같은 서버에서 이 패키지 아래에 병행한다.

## 구조

```
band.gosrock.api.v2
├── common/              # v2 공통 (페이징, 에러 정책)
└── <domain>/
    ├── controller/      # @RequestMapping("/api/v2/...")
    ├── usecase/         # 유스케이스 (도메인 서비스/어댑터 조합)
    └── dto/             # request / response
```

## 네이밍

- v2 클래스는 모두 `V2` 접두사를 붙인다 (`V2EventController`, `V2GetEventUseCase`, `V2EventResponse`).
  - Spring 기본 빈 이름은 단순 클래스명 기반이라, v1과 같은 이름이면 패키지가 달라도 빈 이름 충돌로 기동 실패한다.
- v2 컨트롤러 경로는 반드시 `/api/v2/` 로 시작한다 (Swagger `v2` 그룹, 403 정책이 경로 기준).

## 공통 동작

- 성공 응답: 기존 `SuccessResponseAdvice` 래핑을 그대로 사용 (`{success,status,data,timeStamp}`).
- 에러 응답: 기존 `ErrorResponse` 포맷 그대로. 단, 호스트 권한 실패(`V2ErrorPolicy.HOST_FORBIDDEN_CODES`)는 v2 핸들러(이 패키지의 컨트롤러)에서만 HTTP 403.
  - 403 정책은 MVC 핸들러 예외(`GlobalExceptionHandler`)에만 적용된다. 필터 단계에서 나는 예외는 대상이 아니다.
- 페이징: `V2PageResponse` 하나로 통일. `Slice` 기반이면 `totalElements`/`totalPages`가 `null`.
- 공개(비인증) 경로: `SecurityConfig.V2_PUBLIC_GET_PATHS` 에 추가.
