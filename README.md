# ZeroStar 별 키우기

안드로이드 방치형 게임의 기획 및 구현 인수인계 문서다. 작은 별이 자라고, 완성한 별을 도감에 남기는 무료·광고 포함 게임을 만든다. 게임 진행은 오프라인에서도 가능하며 광고에는 네트워크를 사용한다.

작성일: 2026-10-12 · 상태: 로컬 구현·주요 Android 자동 검증 완료, 독립 추가 검증 진행 대상(IN_PROGRESS). 실기기·사용자·출시 입력은 별도 대기. RELEASE_READY 아님.

## 읽는 순서

1. [게임 방향과 제품 명세](docs/superpowers/specs/2026-10-12-star-game-design.md) — 게임 규칙, 디자인, 기술 선택, 출시 완료 조건.
2. [에이전트 구현 계획](docs/superpowers/plans/2026-10-12-star-game.md) — 작업 순서, 파일 책임, 검증 방법, 재개 및 중단 규칙.
3. [사용자 준비 사항](docs/USER-PREPARATION.md) — 결정할 항목, 계정·기기·키 준비, 출시 절차와 비용.

## 핵심 결정안

- Kotlin + Jetpack Compose로 Android 앱 하나를 만든다. 초기에는 자체 게임 서버와 게임 엔진을 두지 않는다.
- 별빛 자동 생산 → 강화 → 별의 외형 성장 → 도감 등록 → 더 빠른 다음 별 육성이 핵심이다.
- 재화 1종, 강화 1종, 성장 6단계, 수집할 별 6종으로 범위를 제한한다.
- ZeroVerse의 크림색 종이, 자주색 테두리, 산호색 강조, 황혼색 우주와 픽셀 별을 참고한다.
- 무료·인앱결제 없음·광고 포함은 사용자 확정 사항이다. 가장 간단한 광고안은 AdMob 홈 하단 배너 1개다.
- 초기 저장은 이 기기에만 한다. 후속 단계에서 DB 서버 기반 로그인·진행 복구를 구현한다. 초기 출시 완료 조건에는 후속 서버 기능을 넣지 않는다.
- 소스와 문서는 Git으로 관리한다. 원격 저장소 주소·공개 범위는 별도 준비하며 비공개를 권장한다.
- 로그인·랭킹·멀티플레이는 첫 출시에 포함하지 않는다.
- 최종 목표는 실제 기기에서 검증된 서명 AAB와 스토어 자료를 갖추고, 계정별 배포 선행 조건까지 충족한 `RELEASE_READY`다. 실제 공개 버튼 클릭과 Google 심사 통과는 별도 절차다.

사용자가 확정한 조건은 별 키우기, 방치형, 지정 프로젝트의 픽셀 디자인 참고, 간단한 구현, Android 출시 준비 완료, 무료·무결제·광고 포함, 초기 기기 내 저장과 후속 서버 로그인·복구, Git 보관이다. 광고 형식·사업자와 나머지 상세 설계는 기본안이다. 구현을 맡길 때는 구현 계획 마지막의 인수인계 문구를 사용할 수 있다.

## 구현·검증 인수인계

단일 Android 앱에 게임 규칙, DataStore, 홈/도감/설정, 픽셀 자산/음향, UMP와 테스트 배너를 구현했다. 서버·로그인·인앱결제는 포함하지 않는다. API26·API36·실제16KB 에뮬레이터 검증을 실행했다. 실기기2대와 최종 출시 입력은 아직 준비되지 않았다.

- [단계별 진행과 커밋](docs/progress.md)
- [현재 출시 준비 판정](docs/release/RELEASE-READINESS.md)
- [테스트 결과와 APK 해시](docs/qa/TEST-RESULTS.md)
- [재현 빌드·서명·재개 절차](docs/release/RELEASE-RUNBOOK.md)
- [기기·성능·접근성 검사표](docs/qa/DEVICE-MATRIX.md)
- [광고와 동의 검증](docs/qa/ADS.md)

JDK21, Gradle8.13 wrapper, SDK platform36 / build-tools35.0.0을 사용한다. `local.properties`에 SDK 경로를 설정한다. 현재 로컬 SDK는 `.tools/android-sdk`다. 설치와 약관은 사용자 준비 문서 참조.

```powershell
./gradlew.bat -g .tools/gradle-network :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g' '-Pkotlin.compiler.execution.strategy=in-process'
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. 테스트 광고와 임시 `.debug` 패키지이며 출시본이 아니다. 기기 연결 후 `:app:connectedDebugAndroidTest`로 주요 UI·저장 테스트를 실행한다. 실제 장치 실행 없이 구현/빌드 성공을 제품 출시 성공으로 처리하지 않는다.

추가 검증: [OS/광고 경계 결과](docs/qa/OS-EDGE-RESULTS.md). 빌드할 때 작업 AVD를 종료해 Windows 메모리 부족을 피한다. 최종 소스·APK 해시는 TEST-RESULTS를 따른다.
