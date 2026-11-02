#!/usr/bin/env bash
# 릴리스 서명 키를 만들고 GitHub Secret 4개(KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)에 등록한다.
# 한 번만 실행하면 된다. 비밀번호는 화면에 출력하지 않는다.
#
#   bash scripts/setup-release-signing.sh
#
# 필요한 것: JDK(keytool), gh(로그인된 상태), 저장소 관리자 권한
set -euo pipefail

REPO="${REPO:-Jennas-Lee/silver-app-android}"
DIR="${KEYSTORE_DIR:-$HOME/.silver-app}"
JKS="$DIR/release.jks"
PW_FILE="$DIR/release-keystore-password.txt"
ALIAS="silver"

if [ -e "$JKS" ]; then
  echo "이미 서명 키가 있습니다: $JKS"
  echo "같은 키로 Secret만 다시 등록하려면 REUSE=1 을 붙여 실행하세요."
  [ "${REUSE:-}" = "1" ] || exit 1
else
  mkdir -p "$DIR"
  # 영문·숫자 32자 비밀번호
  PW="$(head -c 64 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)"
  printf '%s' "$PW" > "$PW_FILE"
  keytool -genkeypair -keystore "$JKS" -storetype PKCS12 -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PW" -keypass "$PW" \
    -dname "CN=Silver App, O=Jennas-Lee, C=KR" >/dev/null 2>&1
  echo "서명 키를 만들었습니다: $JKS"
fi

PW="$(cat "$PW_FILE")"
base64 -w0 "$JKS" | gh secret set KEYSTORE_BASE64 -R "$REPO"
printf '%s' "$PW"    | gh secret set KEYSTORE_PASSWORD -R "$REPO"
printf '%s' "$ALIAS" | gh secret set KEY_ALIAS -R "$REPO"
printf '%s' "$PW"    | gh secret set KEY_PASSWORD -R "$REPO"

echo "GitHub Secret 4개를 등록했습니다."
echo
echo "중요: 아래 두 파일을 안전한 곳(비밀번호 관리자, 개인 클라우드 등)에 꼭 백업하세요."
echo "      잃어버리면 같은 앱으로 업데이트를 설치할 수 없습니다."
echo "  - $JKS"
echo "  - $PW_FILE"
