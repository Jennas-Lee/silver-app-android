# silver-app-android

어르신을 위한 악성 메시지·보이스피싱 탐지 및 키오스크 음성 안내 앱입니다. Kotlin과 Jetpack Compose로 만듭니다.

- 백엔드 저장소: [silver-app-backend](https://github.com/Jennas-Lee/silver-app-backend)
- API 명세: [silver-app-backend/docs/api/v1.md](https://github.com/Jennas-Lee/silver-app-backend/blob/main/docs/api/v1.md)

## 로컬 개발

1. Android Studio로 이 폴더를 엽니다 (JDK 17).
2. `local.properties`에 서버 주소와 토큰을 넣습니다 (커밋되지 않음).
   ```properties
   # 에뮬레이터에서 PC의 로컬 백엔드(npm run dev)에 붙을 때
   api.baseUrl=http://10.0.2.2:3000
   device.token=dev-token-1
   ```
3. `app`을 실행합니다. 홈 화면에 "서버와 연결됐어요"가 나오면 준비 완료입니다.

```bash
./gradlew testDebugUnitTest   # 단위 테스트
./gradlew lintDebug           # Android Lint
./gradlew assembleDebug       # 디버그 APK
```

## 빌드와 배포 (GitHub Actions)

| 워크플로 | 실행 시점 | 결과 |
|---|---|---|
| `ci.yml` | PR | lint, 단위 테스트, 디버그 빌드 |
| `build-debug.yml` | `main` push | 디버그 APK를 Actions Artifact로 업로드 |
| `release.yml` | `v*` 태그 push | 서명된 APK/AAB를 GitHub Release에 업로드 |

릴리스는 `git tag v0.1.0 && git push origin v0.1.0`으로 만듭니다. `versionCode`는 Actions 실행 번호로 자동 증가합니다.

**GitHub 설정**
- Variables: `API_BASE_URL` (Vercel 프로덕션 주소)
- Secrets: `DEVICE_TOKEN`, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

키스토어 만들기:
```bash
keytool -genkeypair -v -keystore release.jks -alias silver -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # 출력값을 KEYSTORE_BASE64에 등록
```
키스토어 파일은 저장소에 올리지 말고 안전한 곳에 따로 보관하세요. 잃어버리면 같은 앱으로 업데이트할 수 없습니다.

## 예정된 권한

| 권한 / 역할 | 용도 | 단계 |
|---|---|---|
| `INTERNET` | 서버 통신 | 0 ✅ |
| `CAMERA`, `RECORD_AUDIO` | 키오스크 촬영, 음성 질문 | 2 |
| 알림 접근 (`NotificationListenerService`) | 문자·카카오톡 감시 | 4 |
| `READ_CONTACTS` | 연락처에 있는 발신자 제외 | 4 |
| 통화 스크리닝 역할 (`ROLE_CALL_SCREENING`) | 수신 전화 번호 판정 | 4 |
| `<queries>` 원격제어 앱 패키지 | 통화 후 원격제어 앱 설치 감지 | 4 |

## 기여 방법

- 브랜치: `main`에서 `feat/`, `fix/`, `chore/`, `docs/` 접두어로 생성
- 커밋: [Conventional Commits](https://www.conventionalcommits.org/) (`feat(kiosk): ...`)
- PR은 squash merge합니다. 백엔드 API 변경이 필요하면 백엔드를 먼저 배포합니다.
