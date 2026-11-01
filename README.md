# QuickTimestampLog00

홈 화면 위젯을 통해 현재 상황을 Google 캘린더에 빠르게 기록하는 Android 앱입니다.

## 목표 기능

- 위젯을 눌러 일정 입력 화면 열기
- 현재 시각을 기본값으로 사용하고, 날짜와 시간을 수정할 수 있도록 제공
- 제목과 저장할 캘린더 선택
- 일정 알림 없이 저장
- 최근 사용한 제목을 다시 선택하고 관리
- 앱 아이콘 및 입력 화면에서 설정 화면 열기

## 개발 환경

- Android / Kotlin
- Jetpack Compose 및 Material 3
- Android Studio 또는 Gradle을 지원하는 IDE

## 빌드 및 테스트

Windows PowerShell에서 다음을 실행합니다.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## Codex 참고자료

프로젝트 작업에 사용하는 Codex 지침 및 참고자료는 [`codex/`](codex/) 디렉터리에 둡니다.
