# silver-app-android 작업 안내 (AI 에이전트용)

어르신용 앱 "실버 도우미"의 안드로이드 앱입니다. Kotlin + Jetpack Compose로 만듭니다.

- **문자 지킴이:** 사기 문자·카톡 알림을 감지해 알림을 치우고 격리함에 보관
- **키오스크 도우미:** 카메라로 키오스크 화면을 찍으면 어디를 누를지 음성으로 안내
- **계정:** 처음 실행할 때 어르신(이름만 입력) / 보호자(이메일·비밀번호)를 고릅니다. 보호자는 어르신 휴대폰에 나온 숫자 6자리로 연결하고, 어르신이 차단한 문자 목록을 봅니다. **보호자 휴대폰에서는 문자 지킴이와 키오스크가 동작하지 않습니다.**

- 백엔드 저장소: [silver-app-backend](https://github.com/Jennas-Lee/silver-app-backend). 두 저장소를 같은 상위 폴더에 나란히 clone하면 편합니다.
  ```
  silver-app/
  ├── silver-app-backend/
  └── silver-app-android/
  ```
- API 명세: [silver-app-backend/docs/api/v1.md](https://github.com/Jennas-Lee/silver-app-backend/blob/main/docs/api/v1.md). 서버와의 약속이므로 `data/remote/SilverApi.kt`를 고치기 전에 확인합니다.
- 사용자와는 **한국어**로 소통합니다. 짧고 쉬운 말로 설명합니다.

## 새 PC에서 처음 세팅

1. **JDK 17 이상** (출력 바이트코드는 17로 고정되어 있어서 더 높은 버전도 됩니다)
2. **Android SDK.** Android Studio 없이 명령줄 도구만으로도 됩니다.
   - [명령줄 도구](https://developer.android.com/studio#command-line-tools-only)를 받아 `<SDK>/cmdline-tools/latest/`에 풉니다. Windows 기본 위치는 `%LOCALAPPDATA%\Android\Sdk`입니다.
   - 필요한 것을 설치합니다. 플랫폼 버전은 `app/build.gradle.kts`의 `compileSdk`에 맞춥니다.
     ```bash
     sdkmanager "platform-tools" "platforms;android-<compileSdk>" "build-tools;<버전>"
     ```
     (`sdkmanager`가 새 `android` 명령으로 바꾸라고 안내하면 그 안내를 따릅니다.)
3. **`local.properties`** 를 저장소 최상위에 만듭니다. git에 올라가지 않습니다.
   ```properties
   sdk.dir=C\:/Users/<사용자>/AppData/Local/Android/Sdk
   # 에뮬레이터에서 PC의 로컬 백엔드(npm run dev)를 쓸 때. 배포된 서버를 쓰려면 Vercel 주소(https://...)를 넣습니다.
   api.baseUrl=http://10.0.2.2:3000
   ```
   앱은 로그인 토큰으로만 서버를 씁니다. 어르신 계정은 보호자와 연결해야 기능이 열립니다.
4. 빌드가 되는지 확인합니다.
   ```bash
   ./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
   ```

## 작업을 끝내기 전에 반드시 실행

CI(`.github/workflows/ci.yml`)와 같은 검사입니다. 모두 통과해야 PR을 올립니다.

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## 에뮬레이터로 화면 확인

Android Studio 없이 만들 수 있습니다.

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n silver_phone -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd silver_phone -memory 2048
./gradlew installDebug
```

- 디버그 앱의 패키지 이름은 `io.github.jennaslee.silverapp.debug`입니다.
- 화면 캡처: `adb exec-out screencap -p > screen.png`
- 문자 지킴이 시험 (권한을 주고 가짜 문자 보내기):
  ```bash
  adb shell cmd notification allow_listener io.github.jennaslee.silverapp.debug/io.github.jennaslee.silverapp.guard.SmsGuardService
  adb shell pm grant io.github.jennaslee.silverapp.debug android.permission.RECEIVE_SMS
  adb shell pm grant io.github.jennaslee.silverapp.debug android.permission.POST_NOTIFICATIONS
  adb emu sms send +447700900123 "[Web발신] 택배 주소 불일치로 보류중입니다. 확인 bit.ly/3xYzAbc"
  ```
- PC 메모리가 적으면 에뮬레이터와 Gradle 빌드가 겹칠 때 메모리 부족으로 빌드가 죽을 수 있습니다. 이럴 때는 에뮬레이터를 끄고 빌드한 뒤 다시 켭니다.

## 구조

| 위치 | 내용 |
|---|---|
| `MainActivity.kt` | 계정 유형에 따라 화면을 나눔 (처음 실행 / 어르신 홈·키오스크 / 보호자) |
| `SilverApp.kt` | 앱 시작 시 로그인 정보·격리함 불러오기, 기록 업로드 |
| `link/` | 링크 지킴이. `LinkGuardService`(접근성 서비스, 인터넷 창 주소창 읽기 → 위험하면 뒤로 가기), `UrlExtractor`(주소창 찾기, 단위 시험 있음), `LinkChecker`(서버 판정 + 잠시 기억), `LinkStore`(막은 주소), `LinkBlockedActivity`(경고 화면). 볼 앱 목록은 `res/xml/link_guard_config.xml` |
| `session/SessionStore.kt` | 로그인 정보(토큰, 계정 유형) 저장 |
| `guard/` | 문자 지킴이. `SmsReceiver`(문자 원문), `SmsGuardService`(알림 감시), `MessageRules`(1차 규칙), `PiiMasker`(개인정보 가림), `MessageGuard`(판정 흐름), `GuardStore`(격리함), `GuardNotifier`(경고 알림), `RecordSync`(차단 기록을 서버에 올려 보호자와 공유) |
| `kiosk/` | 키오스크 도우미. 카메라(`KioskScreen`), 사진 줄이기(`KioskImage`), 음성(`Speaker`) |
| `ui/` | 어르신 홈, 기록 상세, 테마 |
| `ui/onboarding/` | 처음 실행: 유형 선택, 어르신 시작, 보호자 로그인·가입 |
| `ui/senior/` | 보호자 연결 화면(`LinkRequiredScreen`, 연결 전에는 이 화면만 보임), 어르신 홈의 보호자 연결 카드 (숫자 6자리), 링크 지킴이 카드 |
| `ui/guardian/` | 보호자 화면: 어르신 목록, 차단 문자·보이스피싱 내역, 구독 플랜 고르기(`SubscriptionScreen`, 아직 결제 없음) |
| `data/remote/SilverApi.kt` | Retrofit API와 요청·응답 형식 |
| `app/src/test/` | 단위 테스트 (`MessageRulesTest`, `PiiMaskerTest` 등) |
| `gradle/libs.versions.toml` | 라이브러리 버전 |

## 코드 규칙

- **어르신이 보는 화면과 듣는 말**
  - 쉬운 해요체, 짧은 문장으로 씁니다.
  - IT 용어와 영어를 쓰지 않습니다. 예: "URL" → "인터넷 주소", "앱을 설치" → "프로그램을 까는 것"
  - 글자는 크게, 누르는 곳은 48dp 이상으로 만듭니다. 기존 `BigButton`과 `SilverTheme`의 글자 크기를 씁니다.
- **정규식에 `\b`를 쓰지 않습니다.** JDK와 안드로이드가 한글을 다르게 처리해서, PC 테스트는 통과하고 휴대폰에서는 틀릴 수 있습니다. 대신 앞뒤 확인을 씁니다.
  - 숫자 경계: `(?<!\d)` … `(?!\d)`
  - 영문·링크 경계: `(?<![a-z0-9_])` … `(?![a-z0-9_])`
- `MessageRules`나 `PiiMasker`를 고치면 테스트도 같이 추가합니다. 놓친 예시는 회귀 테스트로 남깁니다.
- 서버로 문자를 보낼 때(판정 요청, 기록 업로드 모두)는 반드시 `PiiMasker`로 계좌·카드·주민번호 등을 가린 뒤 보냅니다. 서버의 `src/lib/pii.ts`와 같은 규칙을 유지합니다.
- **문자 지킴이·키오스크 코드를 고치면** 보호자 계정에서는 동작하지 않는지(`SessionStore.isGuardian`) 확인합니다.
- 로그인하면 `Authorization: Bearer` 토큰으로, 로그인 전에는 예전 `X-Device-Token`으로 요청합니다(`SilverApi.kt`의 인터셉터). 서버 오류 문구는 `ApiClient.errorMessage()`로 보여 줍니다.
- 주석은 한국어로, 짧게 씁니다.
- 설정값(서버 주소, 토큰)은 코드에 박지 않고 `BuildConfig`로 받습니다. 우선순위는 환경변수(CI) > `local.properties` > 기본값입니다.

## 1차 규칙 평가 (선택)

[jmjmjm3/kor-smishing-message](https://huggingface.co/datasets/jmjmjm3/kor-smishing-message) 데이터셋으로 `MessageRules`를 평가합니다. 데이터셋은 비영리 라이선스라 **저장소에 넣지 않습니다.**

```bash
# smishing.csv를 ../.data/kor-smishing/ 에 두거나 SMISHING_CSV 환경변수로 경로 지정
./gradlew testDebugUnitTest --tests '*RulesDatasetEval*'
# 결과: app/build/reports/rules-eval.txt
```

## 빌드와 배포 (GitHub Actions)

| 워크플로 | 실행 시점 | 결과 |
|---|---|---|
| `ci.yml` | PR | lint, 단위 테스트, 디버그 빌드 |
| `build-debug.yml` | `main` push | 디버그 APK를 Actions Artifact로 업로드 |
| `release.yml` | 제목이 `Release vX.Y.Z`인 PR merge (또는 태그 push) | 서명된 APK/AAB를 GitHub Release에 업로드 |

- 릴리스 PR 제목은 정확히 `Release v1.2.3` 형식이어야 합니다. 이미 있는 버전이면 실패합니다.
- GitHub Variables·Secrets(`API_BASE_URL`, 서명 키)는 사용자가 직접 등록합니다(서명 키는 `scripts/setup-release-signing.sh`). 에이전트가 값을 쓰거나 읽지 않습니다.

## Git 규칙

- `main`에 직접 push하지 않습니다. `feat/`, `fix/`, `chore/`, `docs/` 접두어로 브랜치를 만듭니다.
- 커밋 메시지: Conventional Commits, 설명은 한국어. 예: `feat(sms): 문자 지킴이`
- PR은 `.github/pull_request_template.md`를 채우고, **squash merge**합니다.
- PR 여러 개를 이어서 쌓았다면, 아래 PR을 `--delete-branch`로 merge하면 위 PR이 닫힙니다. 위 PR의 base를 먼저 `main`으로 바꿉니다.

## 커밋하면 안 되는 것

- `local.properties`, 키스토어(`*.jks`, `*.keystore`)
- 외부 데이터셋, 평가 결과
- JVM 오류 기록(`hs_err_pid*.log`, `replay_pid*.log`)

## Windows에서 작업할 때

- Gradle은 `gradlew.bat`을 씁니다. Git Bash에서는 `./gradlew`도 됩니다.
- lint가 파일 잠금 오류로 실패하면 `./gradlew --stop`으로 데몬을 끄고 `--no-parallel`을 붙여 다시 실행합니다.
- Git Bash에서 `/sdcard/...` 같은 인자가 Windows 경로로 바뀌면 `MSYS_NO_PATHCONV=1`을 앞에 붙입니다.
