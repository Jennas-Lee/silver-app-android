# silver-app-android

어르신을 위한 악성 메시지·보이스피싱 탐지 및 키오스크 음성 안내 앱입니다. Kotlin과 Jetpack Compose로 만듭니다.

- 백엔드 저장소: [silver-app-backend](https://github.com/Jennas-Lee/silver-app-backend)
- API 명세: [silver-app-backend/docs/api/v1.md](https://github.com/Jennas-Lee/silver-app-backend/blob/main/docs/api/v1.md)

## 로컬 개발

1. Android Studio로 이 폴더를 엽니다 (JDK 17).
2. `local.properties`에 서버 주소를 넣습니다 (커밋되지 않음).
   ```properties
   # 에뮬레이터에서 PC의 로컬 백엔드(npm run dev)에 붙을 때
   api.baseUrl=http://10.0.2.2:3000
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
| `release.yml` | 제목이 `Release vX.Y.Z`인 PR merge (또는 `vX.Y.Z` 태그 push) | 서명된 APK/AAB를 GitHub Release에 업로드 |

### 릴리스 만들기
1. 바뀐 내용을 `main`에 모두 merge합니다.
2. 아무 변경(예: 이 README의 변경 기록)으로 PR을 열고, 제목을 **`Release v0.1.0`** 처럼 씁니다. 대소문자와 띄어쓰기까지 정확해야 합니다.
3. PR을 merge하면 Actions가 서명된 APK/AAB를 만들어 `v0.1.0` Release와 태그를 자동으로 만듭니다.

- 같은 버전이 이미 있으면 실패합니다. 버전 번호를 올려 주세요.
- `versionName`은 제목의 숫자(`0.1.0`), `versionCode`는 Actions 실행 번호로 자동 증가합니다.
- 태그를 직접 push해도 됩니다: `git tag v0.1.0 && git push origin v0.1.0`

### GitHub 설정
- Variables: `API_BASE_URL` (Vercel 프로덕션 주소)
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

### 서명 키 등록 (처음 한 번)
```bash
bash scripts/setup-release-signing.sh
```
서명 키를 만들어 `~/.silver-app/`에 저장하고, 서명용 Secret 4개를 GitHub에 등록합니다. 비밀번호는 화면에 나오지 않습니다.
**`~/.silver-app/`의 두 파일은 꼭 따로 백업하세요.** 잃어버리면 이미 설치된 앱을 업데이트할 수 없고, 지우고 새로 설치해야 합니다.

## 권한

| 권한 / 역할 | 용도 | 상태 |
|---|---|---|
| `INTERNET` | 서버 통신 | ✅ |
| 알림 접근 (`NotificationListenerService`) | 카카오톡 판정, 사기 문자 알림 치우기 | ✅ |
| `RECEIVE_SMS` | 문자 원문 확인 (아래 참고) | ✅ |
| `POST_NOTIFICATIONS` | 위험한 문자 경고 알림 (Android 13+) | ✅ |
| `READ_CONTACTS` (선택) | 연락처에 있는 발신자 제외 | ✅ |
| `CAMERA` | 키오스크 화면 촬영 (음성 질문은 휴대폰 기본 음성 입력 창을 써서 마이크 권한 불필요) | ✅ |
| 통화 스크리닝 역할 (`ROLE_CALL_SCREENING`) | 수신 전화 번호 판정 | 4 |
| `<queries>` 원격제어 앱 패키지 | 통화 후 원격제어 앱 설치 감지 | 예정 |

### 문자 지킴이 동작과 제약
- **흐름:** 문자 도착(`SmsReceiver`) 또는 카톡 알림(`SmsGuardService`) → 1차 규칙(`MessageRules`) → 의심이면 개인정보를 가려(`PiiMasker`) 서버 판정 → 사기면 알림을 치우고 격리함에 저장, 경고 알림
- **서버 판정 실패 시:** 규칙이 고위험일 때만 경고하고, 알림은 치우지 않습니다.
- **원본은 남습니다:** 알림만 치울 수 있고 문자함·카톡방의 원본 메시지는 지워지지 않습니다.
- **Android 15+ 알림 가림:** 숫자가 든 문자 알림은 다른 앱에서 내용이 가려지므로, 문자는 `RECEIVE_SMS`로 원문을 받습니다. 카카오톡 알림도 가려지면 판정하지 못합니다.
- **구글 플레이 정책:** `RECEIVE_SMS`는 제한 권한이라 PoC(직접 설치)용입니다. 출시 전 정책 검토가 필요합니다.
- **직접 설치한 APK (Android 13+):** 알림 접근을 켜려면 설정 → 앱 → 실버 도우미 → 오른쪽 위 메뉴 → "제한된 설정 허용"을 먼저 해야 할 수 있습니다.

### 에뮬레이터에서 시험하기
```bash
adb shell cmd notification allow_listener io.github.jennaslee.silverapp.debug/io.github.jennaslee.silverapp.guard.SmsGuardService
adb shell pm grant io.github.jennaslee.silverapp.debug android.permission.RECEIVE_SMS
adb shell pm grant io.github.jennaslee.silverapp.debug android.permission.POST_NOTIFICATIONS
adb emu sms send +447700900123 "[Web발신] 택배 주소 불일치로 보류중입니다. 확인 bit.ly/3xYzAbc"
```

### 1차 규칙 평가 (데이터셋)
[jmjmjm3/kor-smishing-message](https://huggingface.co/datasets/jmjmjm3/kor-smishing-message) (CC BY-NC-SA 4.0, 저장소에 포함하지 않음)로 `MessageRules`를 평가합니다.
```bash
# smishing.csv를 받아 ../.data/kor-smishing/ 에 두거나 SMISHING_CSV 환경변수로 경로 지정
./gradlew testDebugUnitTest --tests '*RulesDatasetEval*'
# 결과: app/build/reports/rules-eval.txt (놓친 스미싱, 잘못 경고한 정상 예시 포함)
```

| 지표 (17,595건) | 결과 |
|---|---|
| 스미싱을 놓침 (SAFE) | 1.6% (5,677건 중 92건) |
| 정상을 서버로 보냄 | 20.2% (비용에만 영향) |
| 정상을 고위험으로 봄 (서버 실패 시 잘못 경고) | 0.4% |

데이터셋에 보낸 사람 정보가 없어 모두 "연락처에 없는 국내 번호"로 가정했습니다. 규칙을 바꾸면 이 평가를 다시 돌려 숫자가 나빠지지 않았는지 확인하세요.

## 기여 방법

- 브랜치: `main`에서 `feat/`, `fix/`, `chore/`, `docs/` 접두어로 생성
- 커밋: [Conventional Commits](https://www.conventionalcommits.org/) (`feat(kiosk): ...`)
- PR은 squash merge합니다. 백엔드 API 변경이 필요하면 백엔드를 먼저 배포합니다.
