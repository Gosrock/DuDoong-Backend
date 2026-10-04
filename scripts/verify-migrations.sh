#!/bin/bash
# 수동 SQL 마이그레이션 정합 검증 (DEC-017, #721). PR 체크리스트용 — 로컬 docker MySQL(docker-compose) 기준.
#
#   1) 새 DB(${DB}) 에 db/schema/baseline-*.sql + db/migration/V*.sql 을 번호 순서대로 적용
#   2) 앱을 ddl-auto=validate 로 ${DB} 에 붙여 기동 시도 → 결과(성공 / Schema-validation 메시지) 출력
#   3) 엔티티 ↔ DB 대조: 앱을 ddl-auto=create 로 빈 DB(${DB}_entity) 에 띄워 엔티티 기준 스키마를 만들고,
#      information_schema 로 두 DB 의 컬럼(이름·타입·nullable)을 비교한다.
#      - v2 대상(마이그레이션이 만든 테이블 + ADD COLUMN 한 컬럼)의 차이 = 실패
#      - 그 밖(baseline = 기존 prod 구조)의 차이 = 참고 출력만 (v1 시절부터의 차이, 이 스크립트 범위 밖)
#      - 타입은 대략 비교: MySQL 기본 타입(varchar/bigint/datetime…)만 보고 길이는 참고. Hibernate 의 enum(...) 은 varchar 와 같은 것으로 본다
#   4) NotificationBulkRepository 와 같은 모양의 native SQL(사전 조회 SELECT, multi-row INSERT, uk 충돌 1062)을 ${DB} 에서 실행
#
# 사용: scripts/verify-migrations.sh                       # DB e2e_721_schema, 포트 18082
#       DB=my_check PORT=18083 scripts/verify-migrations.sh
#       KEEP_DB=1 scripts/verify-migrations.sh             # 검증 후 DB 를 지우지 않음 (E2E 서버를 이 DB 에 붙일 때)
# 필요: docker compose 의 mysql(13306)·redis(6379), Java 21. bootJar 가 없으면 빌드한다 (있으면 그대로 쓰므로 코드 변경 후엔 먼저 bootJar)
# 자격 증명 기본값은 docker-compose.yml 의 로컬 개발용 값이다 (운영 값 아님).
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB="${DB:-e2e_721_schema}"
ENTITY_DB="${DB}_entity"
PORT="${PORT:-18082}"
KEEP_DB="${KEEP_DB:-0}"
CONTAINER="${MYSQL_CONTAINER:-$(docker ps --format '{{.Names}}' | grep -m1 -E 'mysql')}"
ROOT_PW="${MYSQL_ROOT_PASSWORD:-dudoong}"
APP_USER="${MYSQL_APP_USER:-dudoong}"
LOG_DIR="${LOG_DIR:-$ROOT/build/verify-migrations}"
FAIL=0
APP_PID=""

[[ "$DB" =~ ^[a-zA-Z0-9_]+$ ]] || { echo "DB 이름은 영문·숫자·_ 만: $DB"; exit 2; }
[ -n "$CONTAINER" ] || { echo "mysql 컨테이너를 찾지 못함 (docker compose up -d)"; exit 2; }
mkdir -p "$LOG_DIR"

# 결과의 비밀번호 경고 줄만 지운다. 종료 코드는 mysql 의 것 (grep 이 0줄이어도 실패로 보지 않음)
sql() {
  local out rc
  out=$(docker exec -i "$CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$ROOT_PW" -N "$@" 2>&1); rc=$?
  printf '%s\n' "$out" | grep -v 'Using a password' | sed '/^$/d'
  return $rc
}
ok() { echo "  [OK] $*"; }
ng() { echo "  [NG] $*"; FAIL=1; }

stop_app() {
  if [ -n "$APP_PID" ]; then kill "$APP_PID" 2>/dev/null; wait "$APP_PID" 2>/dev/null; APP_PID=""; fi
}
cleanup() {
  stop_app
  sql -e "DROP DATABASE IF EXISTS \`$ENTITY_DB\`" >/dev/null
  [ "$KEEP_DB" = "1" ] || sql -e "DROP DATABASE IF EXISTS \`$DB\`" >/dev/null
}
trap cleanup EXIT

create_db() {
  sql -e "DROP DATABASE IF EXISTS \`$1\`; CREATE DATABASE \`$1\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL ON \`$1\`.* TO '$APP_USER'@'%';"
}

# Java 21
# 순서: JAVA_BIN → java_home -v 21 → Gradle toolchain 이 받은 JDK(~/.gradle/jdks) → PATH 의 java
find_java21() {
  local c
  for c in "$(/usr/libexec/java_home -v 21 2>/dev/null)/bin/java" $(ls -d "$HOME"/.gradle/jdks/*21*/*/Contents/Home/bin/java "$HOME"/.gradle/jdks/*21*/bin/java 2>/dev/null) "$(command -v java)"; do
    [ -x "$c" ] && "$c" -version 2>&1 | grep -q 'version "21' && { echo "$c"; return; }
  done
}
JAVA_BIN="${JAVA_BIN:-$(find_java21)}"
[ -n "$JAVA_BIN" ] || { echo "Java 21 을 찾지 못함: JAVA_BIN=/path/to/java 로 지정"; exit 2; }

JAR=$(ls "$ROOT"/DuDoong-Api/build/libs/DuDoong-Api-*-SNAPSHOT.jar 2>/dev/null | grep -v plain | head -1)
if [ -z "$JAR" ]; then
  (cd "$ROOT" && ./gradlew :DuDoong-Api:bootJar -q --no-daemon) || exit 1
  JAR=$(ls "$ROOT"/DuDoong-Api/build/libs/DuDoong-Api-*-SNAPSHOT.jar | grep -v plain | head -1)
fi

# 앱 기동 → "Started" 또는 프로세스 종료까지 대기 후 종료. $1 = DB, $2 = ddl-auto, $3 = 로그 파일. 성공 시 0
boot() {
  "$JAVA_BIN" -jar "$JAR" --spring.profiles.active=local --server.port="$PORT" \
    --spring.datasource.url="jdbc:mysql://127.0.0.1:13306/$1?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&tinyInt1isBit=false" \
    --spring.jpa.hibernate.ddl-auto="$2" --spring.sql.init.mode=never --spring.jpa.show-sql=false > "$3" 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 180); do
    grep -qE "Started [A-Za-z]+ in [0-9.]+ seconds" "$3" && break
    kill -0 "$APP_PID" 2>/dev/null || break
    sleep 1
  done
  grep -qE "Started [A-Za-z]+ in [0-9.]+ seconds" "$3"; local rc=$?
  stop_app
  return $rc
}

echo "== 1) $DB: baseline + migration 적용 (컨테이너 $CONTAINER)"
create_db "$DB" || exit 1
for f in "$ROOT"/db/schema/baseline-*.sql "$ROOT"/db/migration/V*.sql; do
  out=$(sql "$DB" < "$f")
  if [ -n "$out" ]; then ng "$(basename "$f"): $out"; else ok "$(basename "$f")"; fi
done

echo "== 2) 앱 기동 ddl-auto=validate ($DB, 포트 $PORT)"
if boot "$DB" validate "$LOG_DIR/validate-boot.log"; then
  ok "validate 기동 성공"
  VALIDATE_RESULT="성공"
else
  VALIDATE_RESULT=$(grep -m1 -oE "Schema-validation: [^\"]{0,200}" "$LOG_DIR/validate-boot.log" || echo "기동 실패 (로그 참고)")
  echo "  [INFO] validate 실패: $VALIDATE_RESULT"
  echo "         (baseline 기존 구조 차이일 수 있음 → 3) 의 v2 대조 결과로 판정. 로그: $LOG_DIR/validate-boot.log)"
fi

echo "== 3) 엔티티 ↔ DB 컬럼 대조 ($ENTITY_DB = ddl-auto=create)"
create_db "$ENTITY_DB" || exit 1
if ! boot "$ENTITY_DB" create "$LOG_DIR/entity-boot.log"; then
  ng "ddl-auto=create 기동 실패 (로그: $LOG_DIR/entity-boot.log)"
else
  cols() {
    sql -e "SELECT CONCAT(table_name,'.',column_name), data_type, column_type, is_nullable FROM information_schema.columns WHERE table_schema='$1' ORDER BY 1"
  }
  cols "$ENTITY_DB" > "$LOG_DIR/entity-columns.tsv"
  cols "$DB" > "$LOG_DIR/migration-columns.tsv"
  # v2 대상: 마이그레이션의 CREATE TABLE 테이블 전체 + ALTER TABLE ... ADD COLUMN 컬럼
  V2_TABLES=$(grep -hoiE "CREATE TABLE \`?[a-z_]+" "$ROOT"/db/migration/V*.sql | awk '{print $3}' | tr -d '`' | sort -u)
  V2_COLUMNS=$(python3 - "$ROOT"/db/migration/V*.sql <<'PY'
import re, sys
out = set()
for path in sys.argv[1:]:
    text = re.sub(r"--[^\n]*", "", open(path, encoding="utf-8").read())
    for stmt in text.split(";"):
        m = re.search(r"ALTER\s+TABLE\s+`?(\w+)`?", stmt, re.I)
        if m:
            for c in re.findall(r"ADD\s+COLUMN\s+`?(\w+)`?", stmt, re.I):
                out.add(f"{m.group(1)}.{c}")
print("\n".join(sorted(out)))
PY
)
  python3 - "$LOG_DIR/entity-columns.tsv" "$LOG_DIR/migration-columns.tsv" "$V2_TABLES" "$V2_COLUMNS" > "$LOG_DIR/column-diff.txt" <<'PY'
import sys
def load(p):
    rows = {}
    for line in open(p, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) == 4:
            rows[parts[0]] = parts[1:]
    return rows
entity, migration = load(sys.argv[1]), load(sys.argv[2])
v2_tables, v2_columns = set(sys.argv[3].split()), set(sys.argv[4].split())
norm = lambda t: "varchar" if t == "enum" else t
fail, info = [], []
for key in sorted(set(entity) | set(migration)):
    table = key.split(".")[0]
    scope = fail if (table in v2_tables or key in v2_columns) else info
    e, m = entity.get(key), migration.get(key)
    if e is None:
        if table in {k.split(".")[0] for k in entity}:  # 엔티티에 없는 컬럼 (DB 에만) — 사용 안 하는 과거 컬럼
            scope.append(f"DB 에만 있음: {key} {m[1]} null={m[2]}")
        continue  # 엔티티에 없는 테이블(과거 조인 테이블 등)은 비교하지 않음
    if m is None:
        scope.append(f"DB 에 없음: {key} (엔티티 {e[1]} null={e[2]})")
        continue
    if norm(e[0]) != norm(m[0]):
        scope.append(f"타입: {key} 엔티티 {e[1]} / DB {m[1]}")
    if e[2] != m[2]:
        scope.append(f"nullable: {key} 엔티티 {e[2]} / DB {m[2]}")
    elif e[1] != m[1] and norm(e[0]) == norm(m[0]) and e[0] != "enum":
        info.append(f"(길이 등 참고) {key} 엔티티 {e[1]} / DB {m[1]}")
print(f"FAIL {len(fail)}")
for x in fail: print("  v2  " + x)
print(f"INFO {len(info)}")
for x in info: print("  기존 " + x)
PY
  v2_fail=$(awk '/^FAIL/{print $2}' "$LOG_DIR/column-diff.txt")
  if [ "$v2_fail" = "0" ]; then
    ok "v2 테이블 $(echo "$V2_TABLES" | wc -w | tr -d ' ')개 + 추가 컬럼 $(echo "$V2_COLUMNS" | wc -w | tr -d ' ')개: 엔티티와 이름·타입·nullable 일치"
  else
    ng "v2 대상 차이 ${v2_fail}건:"; grep '^  v2 ' "$LOG_DIR/column-diff.txt"
  fi
  echo "  [INFO] 기존(baseline) 차이 $(awk '/^INFO/{print $2}' "$LOG_DIR/column-diff.txt")건 — $LOG_DIR/column-diff.txt"
fi

echo "== 4) NotificationBulkRepository native SQL ($DB)"
COLS="user_id, type, title, body, target_type, target_id, event_id, extra, dedup_key, is_read, created_at, updated_at"
ROW1="(900001, 'ORDER_APPROVED', '제목', '본문', 'ORDER', 'uuid-1', 1, '{\"k\":\"v\"}', 'uuid-1', false, NOW(), NOW())"
ROW2="(900002, 'ORDER_APPROVED', '제목', '본문', 'ORDER', 'uuid-1', 1, NULL, 'uuid-1', false, NOW(), NOW())"
out=$(sql "$DB" -e "INSERT INTO tbl_notification ($COLS) VALUES $ROW1, $ROW2; SELECT ROW_COUNT();")
[ "$out" = "2" ] && ok "multi-row INSERT 2행" || ng "multi-row INSERT: $out"
out=$(sql "$DB" -e "SELECT user_id, type, dedup_key FROM tbl_notification WHERE user_id IN (900001, 900002) AND dedup_key IN ('uuid-1')" | wc -l | tr -d ' ')
[ "$out" = "2" ] && ok "사전 조회 SELECT 2행" || ng "사전 조회 SELECT: $out"
out=$(sql "$DB" -e "INSERT INTO tbl_notification ($COLS) VALUES $ROW1")
[[ "$out" == *"ERROR 1062"* ]] && ok "uk 충돌 → ERROR 1062 (Spring DuplicateKeyException 대상)" || ng "uk 충돌 기대, 실제: $out"
out=$(sql "$DB" -e "SELECT CONCAT(HEX(title), ':', is_read + 0) FROM tbl_notification WHERE user_id = 900001")
[ "$out" = "ECA09CEBAAA9:0" ] && ok "한글(utf8mb4) 저장 · is_read=0" || ng "인코딩/기본값: $out"
sql "$DB" -e "DELETE FROM tbl_notification WHERE user_id IN (900001, 900002)" >/dev/null

echo
echo "validate: $VALIDATE_RESULT"
[ "$FAIL" = "0" ] && echo "결과: 통과" || echo "결과: 실패 항목 있음"
exit "$FAIL"
