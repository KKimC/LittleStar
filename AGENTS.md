# AGENTS.md

PianoFlow — 갤럭시 탭용 피아노 연습 어시스턴트 Android 앱 개발 가이드.

## 프로젝트 개요

- 언어: Kotlin, UI: Jetpack Compose (Material 3), 아키텍처: MVVM
- 상태 관리: ViewModel + StateFlow, 비동기: Kotlin Coroutines
- DI: 수동 DI (`di/AppContainer`). Hilt 등 프레임워크는 아직 도입하지 않음.
- 데이터: Room(`data/local`) 기반. 악보 이미지 파일은 `data/storage/SheetFileStorage`가 내부 저장소(`filesDir/sheets`)에 관리.
- 스캐너: Google ML Kit Document Scanner(`play-services-mlkit-document-scanner`). GMS 기기 필요.

## 개발 규칙

- Kotlin 공식 스타일을 따른다.
- UI와 데이터 계층을 분리한다.
  - `feature/*`: 화면(Composable) + 화면별 ViewModel
  - `domain/*`: 순수 모델/리포지토리 계약/유스케이스
  - `data/*`: 리포지토리 구현(Room) + 엔티티/DAO/DB
  - `core/*`: 네비게이션, 디자인 시스템, 공용 유틸
  - `di/*`: 의존성 컨테이너
- Composable 함수의 책임을 최소화한다. 작은 단위로 나누어 재사용한다.
- 화면 상태는 반드시 ViewModel에서 관리한다. Composable 내부 임시 UI 상태는
  `remember`/`rememberSaveable`로 제한한다.
- 불필요한 의존성을 추가하지 않는다.
- 기존 기능을 보존한다.
- 파일을 삭제하기 전에 반드시 그 이유를 확인한다.
- 빌드 오류가 남아 있으면 작업을 "완료"로 선언하지 않는다.
- Android 개발 초보자가 이해할 수 있도록 중요한 부분에 주석을 남긴다.

## 빌드

```bash
./gradlew :app:assembleDebug   # Windows: .\gradlew.bat :app:assembleDebug
```

작업 완료 후에는 반드시 위 빌드가 성공하는지 확인한다.

## 설계 노트

- 디자인 토큰은 `core/design/theme/`에 정의되어 있다.
  - Background `#F7F6F0`, Surface `#FFFFFF`, Primary `#718A78`,
    PrimaryContainer `#E3EBE2`, TextPrimary `#303B32`, TextSecondary `#858B82`
- 반응형 레이아웃은 `core/util/WindowSize.kt`의 `rememberWindowSizeClass()`를 사용한다.
  - 가로(넓은) 화면은 통계/보관함을 나란히, 세로 화면은 세로 스크롤로 배치한다.
- 네비게이션 경로는 `core/navigation/Routes.kt`에 정의한다.
- 화면 회전 시 상태가 초기화되지 않도록 상태는 ViewModel에 보관한다.
- Room 엔티티/DAO/DB는 `data/local/`에, 최초 더미 데이터는 `data/local/SeedData.kt`에서 시드한다.
- 날짜/주간 계산은 `java.time`(desugaring 적용)을 사용한다. DB에는 epoch millis를 저장한다.
- 연습 타이머는 monotonic clock(`SystemClock.elapsedRealtime()`) 기반으로 계산하고, 세션 시작/종료 시 wall-clock timestamp와 durationSeconds를 저장한다. 매초 DB 갱신은 하지 않는다.
- 앱 시작 시 `PianoFlowApplication`에서 이전 프로세스의 RUNNING 세션을 INTERRUPTED로 정리한다.
