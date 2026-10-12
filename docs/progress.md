# ZeroStar 진행 기록

기준 계획: docs/superpowers/plans/2026-10-12-star-game.md
상태: IN_PROGRESS. RELEASE_READY가 아님. 2026-10-12.

## 시작과 판단

- 원본 문서 4개 보존: `df14861`. 적용되는 AGENTS.md 및 .agents/skills 없음.
- 구현 브랜치 `implementation/local-release`. 사용자 파일 변경·원격 생성·push 없음.
- Ruling: 새 저장소의 지정 작업 폴더에서 구현 브랜치 사용. 기존 체크아웃 작업이 없으므로 별도 worktree 불필요.
- Ruling: 프로젝트가 요청한 추적 문서인 이 파일을 영구 ledger로 사용. 미검증 단계의 체크박스는 채우지 않는다.
- Pre-flight: M1 모델 → M2 저장 → M3 투영은 밀리별빛 단위 공유. M2의 경과초는 누적값이 아닌 저장 이후 증분이어야 함. M3a는 게임 상태와 독립.
- Android SDK/Studio/adb 기본 경로 없음. 사용자도 설치 필요 확인. SDK 약관을 대신 수락하지 않는다.
- JDK 21.0.10, Git 사용 가능. Gradle 8.13 + AGP 8.13.2 + Kotlin 2.2.21 + Compose BOM 2025.10.01 고정.
- 공식 호환 근거: https://developer.android.com/build/releases/agp-8-13-0-release-notes (API 36.1, Gradle 8.13, JDK 최소 17).
- Ruling: SDK 준비 동안 동일 게임/저장 Kotlin 소스를 JVM에서 검증하는 보조 빌드 사용. 앱 모듈은 하나 유지. Android 검증을 대체했다고 보고하지 않는다.
- 부모 알림 시도: 제공 thread ID가 도구에서 not found. 최종 응답 자동 전달 사용.

## 단계 증거

| 단계 | 상태 | 근거/미완료 |
|---|---|---|
| M0 | 진행 | 원본 커밋, Android 설정. 빌드·설치 미검증 |
| M1~M7 | 대기 | 순차 구현 예정 |

### M1 규칙 검증
- M0 설정 커밋 `52cb0d5`. SDK 없는 최초 Android 빌드는 환경 오류; 설치 실행 미검증.
- 사용자 SDK 약관 동의 완료 확인 후 SDK 설치 진행. adb 연결 기기 0대.
- 규칙/경제 6개 테스트 미구현 RED 6실패 → GREEN 6통과. 첫 완성 8,501초, 4/8/12h 도감 158,580/316,980/475,380초.
- 명령: `.tools/gradle-8.13/bin/gradle.bat -g .tools/gradle-network -p verification test --max-workers=2`.
- 로그: artifacts/logs/m1-red.txt, m1-green.txt. 상세: docs/qa/BALANCE.md.
- Android 테스트는 SDK 설치 후 별도 실행. M1 핵심 규칙 검증 통과, 전체 단계 통과 표시는 Android 빌드 후 갱신.

### M2 저장 코어 검증
- 규칙 커밋 `2d81a03`.
- 실제 임시 파일 DataStore 테스트, 동시 구매/등록, 중복 복귀, 8시간 상한, 시각 역행, 손상/미래 스키마/범위 오류 보존 및 명시 초기화, 쓰기 불가 후 재시도 검증.
- 16개 테스트 중 미구현에서 9실패 → 최종 16통과. 로그 artifacts/logs/m2-red.txt, m2-green.txt. LifecycleSaveTest는 단조 시계 순수 로직이며 Android OS 검증과 구분.
- Ruling: 저장 데이터의 필수 필드 누락도 손상으로 처리. 기본값 역직렬화로 손상 파일이 새 게임이 되는 것을 방지.
- Android 실제 생명주기·강제종료 검증은 뒤따르는 장치 테스트에 남아 있음.

### M3 화면·Android 빌드
- 저장 코어 커밋 `d9fb66e`.
- 3화면, 한국어 리소스, 픽셀 별 6단계/6장식, OFL 폰트 로컬 번들, 원본 합성 음향 3개, 설정·초기화·복귀 안내 구현.
- Android `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` BUILD SUCCESSFUL. 단위 테스트 20개 통과, lint 0 errors / 15 warnings. 로그 artifacts/logs/m3-verify.txt.
- Ruling: Windows에서 Android DataStore의 renameTo가 기존 파일 교체에 실패. 호스트 단위 테스트 런타임만 공식 datastore-core-jvm으로 치환. Android instrumentation에도 동일 코어 테스트 소스를 포함해 실제 Android 변형은 장치에서 검증한다.
- UI 테스트 코드는 구현 전에 작성했지만 실행 환경 준비 중이라 UI RED→GREEN은 아직 입증 못함. 컴파일 실패를 UI 행동 테스트 실패로 세지 않음.
- API36/Build Tools35/adb/에뮬레이터 설치 목록 artifacts/logs/sdk-installed.txt. 사용자 별도 ‘동의 완료’ 응답 기록. `--licenses` 경고 자체는 동의/설치 증거 아님.
- 에뮬레이터 WHPX 지원, 기본 데이터 파티션 공간 부족 확인. 테스트 AVD 2GB로 변경 중. 실기기 0대.

### M3a 광고 연동 검증
- M3 커밋 `0730870`. 공식 Next-Gen SDK 1.5.0 + UMP 4.0.0 사용. Legacy SDK·미디에이션·분석 SDK 추가 없음.
- 동의 게이트 4개 포함 JVM 20개 통과. RED 로그 m3a-red.txt(2실패), GREEN m3a-green.txt.
- 광고 통합 후 Android assembleDebug / testDebugUnitTest / lintDebug / assembleDebugAndroidTest BUILD SUCCESSFUL, 20개 단위 테스트 통과, lint 0 errors / 14 warnings. artifacts/logs/m3a-verify.txt.
- 실제 배너/UMP/오프라인 장치 테스트 미실행. 에뮬레이터가 데이터 영역 최소 6GB(부팅 여유 7.2GB)를 강제해 C: 여유 약 5GB로 차단. 사용자에게 추가 공간/실기기 요청.
- Ruling: 현재 실행 도구에는 Dots 클라우드 터미널이 없고 연결 프로젝트는 local뿐. 사용자 클라우드 질문은 이전/업로드 승인으로 해석하지 않음.
- 출시 packaging은 실제 ID·정책·연령 명시·서명 입력 없으면 실패하도록 구성. debug/benchmark는 공식 테스트 ID.
