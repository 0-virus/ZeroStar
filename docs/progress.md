# ZeroStar 진행 기록

기준 계획: docs/superpowers/plans/2026-10-12-star-game.md
상태: RELEASE_READY 미충족 — TalkBack 입력 프로브 수정·세 화면 초점 통과. 실제 NO_FILL/음성/실기기 검증 잔여. 사용자·출시 입력 WAITING_FOR_USER. RELEASE_READY가 아님. 2026-10-12. 아래는 시간순 기록이며 최신 판정은 마지막 절과 RELEASE-READINESS를 따른다.

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
| M0 | 로컬 검증 통과 | 52cb0d5, 이후 Android 빌드·API26/36 실행 확인 |
| M1 | 구현·자동 검증 통과 | 2d81a03, 규칙/경제 RED→GREEN |
| M2 | 구현·자동 검증 통과, 실기기 장기 검증 대기 | d9fb66e 및9e77ea4, 파일 저장/큐/실제 OS 반복·재부팅 |
| M3 | 구현·주요 UI 및 실제 TalkBack 초점 통과, 음성 검사 대기 | 0730870 및9e77ea4, 작은 화면/설정/도감 수정 |
| M3a | 구현·부분 장치 검증, 실제 동의 모든 분기·계정 대기 | 43f7ffb 및9e77ea4, 테스트 배너/SDK 계약 통과 |
| M4 | 콘텐츠·자료 준비, 실제3명 피드백 대기 | 수치 임의 변경 없음, 스토어 자산/라이선스 |
| M5 | 에뮬레이터 검증, 실기기 품질 기준 대기 | API26 이전32/API36 일반31/최신16KB core35 통과 |
| M6 | 테스트 APK·스토어 자료 준비, 출시 입력/키/AAB 대기 | TEST-RESULTS 해시·RUNBOOK·DEPENDENCIES |
| M7 | 항목별 인수인계, 계정·외부 검증 대기 | RELEASE-READINESS §9 전항목 증거/차단 기록 |

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

### 코드 검토·M4~M6 독립 준비
- M3a 커밋 `43f7ffb`.
- 새 리뷰어가 P1 화면 종료 중 저장 취소, P2 스위치 명칭, 작은 가로 화면의 도감 상세 잘림을 발견. Application 소유 FIFO로 저장 수명을 분리하고, 이름 있는 스위치 행/도감 상세 스크롤로 수정. 게임 타이머도 배경 진입 시 취소.
- 지연 저장/호출자 취소 순서 회귀는 RED 1실패 → GREEN. 기존 파일에 대한 실제 쓰기 실패 보존/재시도도 추가. 호스트 테스트 23개 통과. API36 장치 검증은 아래 별도 기록.
- debug/benchmark APK, debug/release lint, 단위23개, test APK 빌드 성공: artifacts/logs/final-android-verified.txt. 빌드에 사용된 DataStore 1.2.1은 공식 안정 버전의 16KB 링크 수정 확인 후 채택. 새 기능이나 의존성 종류를 추가하지 않음.
- 네이티브 파일 검사에서 기존 DataStore1.1.7 RELRO 끝 정렬 실패를 발견. 1.2.1은 LOAD/RELRO16KB 검사 통과. graphics-path1.0.1은 LOAD16KB/RELRO 추가 검사 불충족. 공식 안정1.1.0도 같아 변경하지 않음. 실제 16KB 실행 전 호환 완료로 표시하지 않는다.
- 근거: https://developer.android.com/guide/practices/page-sizes 및 https://android.googlesource.com/platform/frameworks/support/+/00bc339d026c39791f891ea9db2313c1fcdc97f9 . 검사 도구 tools/inspect-apk.py.
- 스토어 문구·정책·릴리스 노트 초안, 아이콘/대표 이미지, 기기/광고 검증표, 출시 재개 절차 준비. 실제 캡처와 계정/법적 확정은 별도.

### 공간 확보 이후 API36 실제 실행
- 사용자가 공간을 확보했고 C: 15.95GiB를 확인. 기존 API36 AVD 부팅 성공(4KB 페이지). 이전 공간 부족 차단은 해소됐다. 미사용 자체 캐시/중복ZIP만 정리했으며 일부 잠긴 캐시는 남겼다.
- 첫 instrumentation은 30개 중 8개 결과에서 2실패, 전체 미완료. 광고 초기화의 AgeRestrictedTreatment.valueOf 호출이 runtime NoSuchMethodError를 일으킴. 별도로 Windows qemu 프로세스가 0xc0000005로 종료되어 남은 실행 중단. 로그 artifacts/logs/api36-instrumentation.txt, artifacts/qa-run/api36-red.
- 공식 문서의 CHILD/TEEN/UNSPECIFIED 상수 선택으로 수정하고 LinkageError 시 게임을 종료하지 않게 광고 초기화를 실패 처리. 실제 SDK 계약 회귀 테스트 추가. 수정 후 debug/testAPK/단위23/lintDebug/lintRelease BUILD SUCCESSFUL(ads-sdk-fix-build.txt).
- Vulkan을 끈 API36 AVD 부팅 성공, 전체31개 장치 테스트 재실행 중. 성공 여부는 최종 TEST-RESULTS에 기록한다. API26 이미지 설치 진행. 위 오류를 컴파일 통과만으로 해결 완료 처리하지 않는다.

- 실시간 체크포인트: 사용자 두 번째 공간 재확인에서 C 15.95GiB(17.12GB), 이후 API36 userdata/API26 설치 후 약9GiB. API36 31개 재실행 중 10개 완료/실패0 확인. 외부 입력 대기 때문에 멈춘 상태가 아님. 부모 메시지 전달 도구는 thread not found를 반환해 이 파일에 함께 기록.

### 최종 장치 검증 확대
- API36 일반 이미지에서 31개 instrumentation 통과(api36-retest.txt). 실제 화면에서 강화 버튼이 첫 화면 아래로 밀리는 문제를 발견해 60dp 배너를 둔 360×640dp 테스트로 RED를 재현했고, compact-home-green2.txt에서 GREEN 확인. UI 숫자·규칙 변경 없음.
- API26 최종 전체32개 및 unit23/lintDebug/lintRelease 통과(api26-final-suite.txt). 가로640×360·글자1.3배에서 도감 상세/설정 의미, native 두 라이브러리 로딩 추가3개 통과(api26-landscape-font13.txt).
- API36 실제 강화 구매 후 force-stop+재실행20회, 배경/복귀20회 통과. tools/check-os-cycles.py가 저장 레벨·설정 유지, 생산량의 시간 범위, 중복 지출/지급 불변값을 확인. artifacts/qa-run/*20.json 및 로그에 전후 상태 보존. 도감 등록 직후 OS 종료·실기기 검증으로 확대 해석하지 않음.
- 실제 Google Test Ad 화면 확보(growth-360.png, banner-360.png). 배너 WebView [0,488][360,544], 탭 라벨 y572~596으로 경계 분리 확인. 광고 클릭 없음.
- 정상·도감·설정 실제 캡처 확보. API36 호스트 QEMU가 두 차례 0xc0000005로 종료한 이력과 System UI 응답 지연은 별도 환경 이슈로 기록. 비행기 모드 검증은 당시 중단됐음.
- API26 재부팅 전후 강화1 유지, balance64000→307000, epoch 차81.204초에 기존 생산량3별빛/초로243000milli 지급 확인.
- SDK16KB 이미지 설치 및 AVD 준비, 실행 검증 진행 중. 완전 출시 판정은 여전히 실기기/법적·계정/서명/실제AAB 조건 대기.

### 최종 인수인계 (2026-10-12 UTC)

- 제품 코드·회귀 테스트·도구 정리 커밋 `9e77ea4`. 미해결 사용자 변경 덮어쓰기, 원격 push, 공개, 결제 없음.
- 컨텍스트 오류 후 기존 로그/프로세스를 확인했다. final-16kb-and-build.txt는 BUILD SUCCESSFUL(6분30초). PAGE_SIZE16384 장치 테스트33개 전부 통과, 실패/오류/건너뜀0. Android 단위23개, debug/release lint 각각 오류0·경고13, R8 benchmark 빌드 통과. 네이티브 두 라이브러리를 실제16KB x86_64에서 직접 로딩했다.
- 최종 debug/benchmark APK는 apksigner·zipalign16KB 검증 exit0. 실제 의존성 그래프·SHA-256·파일 크기·인증서 공개 지문을 문서화했다. release 가드는 미확정 applicationId를 기대한 실패로 차단했다. 출시 AAB나 실제 키는 생성하지 않았다.
- API26에서 기본 네트워크 경로 제거/ping Network is unreachable을 확인한 뒤 강화1→2가 실제 저장됨을 확인했다. 이전 svc wifi/data 명령만의 시도는 단절 증거가 아니므로 제외했다. 네트워크를 복구하고 에뮬레이터를 종료했다.
- 이 작업용 API26 이미지·AVD만 경로 확인 후 제거해 C:0.63GiB→11.44GiB 확보. 기존 API26·일반API36 검사 로그/화면은 보존하고16KB AVD는 유지했다. 사용자 파일을 삭제하지 않았다.
- M4 디자인 비교는 ZeroVerse DESIGN-SYSTEM §1~5/CSS와 실제 Android 홈/도감/설정 캡처의 문서·코드 기반 비교다. 크림 종이·자주 테두리·산호 강조·황혼 배경·로컬 글꼴·픽셀 별을 대조했다. ZeroVerse를 실행한 동시 화면 비교는 아니며 원본 수정 없음. 별6종 장식과 색, 잠금/조건은 소스·도감 UI 검증으로 확인했다. 실제3명 피드백은 미확보여서 M4 완료 아님.
- 사용자 행동: 우선 테스트 가능한 실기기2대와 실제 플레이 참여자3명 확보. 그와 병행해 최종 이름/패키지/버전/국가/연령/지원 이메일/공개 정책 URL, 업로드 키 보관, Play·AdMob 계정 권한과 실제 광고 ID/UMP 설정 준비. 비밀은 채팅·Git에 넣지 않는다.
- 다음 실행은 DEVICE-MATRIX의 미검증 실기기·TalkBack·실제8h·광고 분기 검사, 이후 RUNBOOK의 최종 입력 가드→서명 AAB→동일 AAB 설치·업데이트→내부 테스트·보고서 순서다. 현 상태 WAITING_FOR_USER, RELEASE_READY 아님. 계정별 실제 테스트 기간/심사는 조건 확인 후 WAITING_FOR_EXTERNAL로 기록한다.

- 추가 레이아웃: 412×892dp UI7개, 600×960dp/글자1.3 UI7개 모두 통과(ui-412dp-installed.txt, ui-600dp-font13.txt). 첫 adb 호출의 runner 없음은 테스트 APK 정리 상태였고 최종 APK 재설치로 해결. 설정 복구 후 종료한다.

- 최종 benchmark APK 실제16KB 설치·MainActivity 실행·프로세스 유지 확인. System UI 응답 없음 대화상자 관찰은 별도 기록했고 실기기 안정성 합격으로 해석하지 않았다. 모든 검사 후 화면 설정 복구, 작업용 에뮬레이터 종료.

### 마지막 인수인계 감사

부모 요청으로 §9/M0~M3a와 최종 코드·lint를 읽기 재검토했다. 사용자 준비 없이 가능한 추가 OS/광고/접근성 검증이 남아 있으므로 모든 잔여 작업을 WAITING_FOR_USER로 묶은 이전 요약을 정정했다. 독립 검증 목록은 RELEASE-READINESS의 인수인계 감사 절에 명시한다. M0~M3a 전체 수동 검증 완료라고 판정하지 않는다.

독립 수정: release 가드가 최종 버전 입력 없이 default0.1.0/1을 허용하던 빈틈을 발견했다. 명시적 versionCode/versionName 검사를 추가하고, 실제 자격증명 없는 회귀3건으로 RED(2실패/1통과)→GREEN을 검증한다. APK 안의 게임 런타임은 수정하지 않았으며 이전 APK 해시·서명·기기 검사 근거는9e77ea4 빌드에 귀속된다.

lintDebug/release 각13개는 버전 권고12 + Uri KTX 권고1. 별도 런타임 결함 경고는 없다. 이 사실이 미실행 경로의 결함0을 보장하지는 않는다. 확인된 이전 저장/SDK/UI 결함은9e77ea4에서 수정·회귀했고 System UI/호스트 문제와 ARM64 최종 검증은 기존 기록대로 남긴다.

- 후속 가드 회귀3건 GREEN, Android unit23/lintDebug·release0오류/13경고 유지. 테스트 APK3개 모두 Android Debug/v2 인증서 확인. 검증 명령42초 BUILD SUCCESSFUL. 기존 APK 해시 변경 없음.

### 독립 장치 검증 재개

- 출시 가드/인수인계 보완 커밋 `7e4ddca`. API26 이미지를 다시 설치하고 OS/UI 검사를 확대했다. 등록 직후 종료20/20, 실제 저장 오류4종·복구, OS 시계+1h/-2h/+24h, 확실한 IPv4/IPv6 차단에서 게임 구매와 UMP 실패 및 복구, 광고 탭20회/회전4회, 분할 화면+글자1.3 통과. 자세한 fixture/로그/한계는 [OS-EDGE-RESULTS](qa/OS-EDGE-RESULTS.md).
- wlan0만 내린 과거 오프라인 증거는 전체 통신 차단 증거로 불충분했다. 실제 SDK 요청 실패까지 확인한 후속 검사로 정정한다. 시계 변경으로 실제8h 경과를 대체하지 않는다.
- 소프트웨어 GPU의 QEMU 종료·Windows commit 한도 소진을 관찰했다. 사용자 프로세스는 건드리지 않고 작업 AVD만 host GPU/작은 guest RAM으로 재시작했다. AVD를 끄고 worker1/JVM1GB로 빌드한다. 디스크와 호스트 메모리 원인을 구분한다.
- 추가 접근성/개인정보 진입 검사를 포함해 실제16KB core35 통과(13.75초). 단위23, debug/release lint 오류0/경고13 및 test APK 빌드 성공(1분15초). SDK 지역/오류 진단은 opt-in 별도이며 core 합격 수에 넣지 않는다.
- API26 실제 UMP 테스트 지역 불필요/필요/거부 결과 확인. API36 개인정보 양식 재열기 시도의 거부 선택은 자동 승인 검토가 법적 입력 경계라며 거부했고, 사용자에게 테스트 거부 선택만 명시적 허용을 요청했다. 우회하거나 수락 선택하지 않는다.
- 최신 사용자 공간 재확인: C:5.63GiB. API26와16KB AVD/이미지는 이미 설치되어 있고16KB는 정상 부팅했다. 현재 공간 부족이 부팅 차단은 아니다. 추가 대용량 설치 전에는 다시 확인한다.


### 최종 증거 연결

- 검증 도구·UI 회귀·실패 프로브·OS 증거 커밋 `d55d904`. 다음 커밋은 문서 정리만이며 이 소스를 검증한 APK와 연결한다.
- 최종 소스에서 debug/test/benchmark 빌드·unit23·lint 각각0오류/13경고 통과(46초). 출시 버전 입력 가드3/3 통과. APK3개 v2 Android Debug 서명 확인, debug/benchmark ELF LOAD/ZIP16KB 통과. 최신 해시는 TEST-RESULTS 마지막 절.
- 최종 APK 재설치 후 실제16KB core35 통과(35.379초). 최적화 benchmark 설치·정상 홈/테스트 배너 확인, crash buffer 비어 있음. 단발 관찰을 실기기30분/성능 합격으로 취급하지 않는다.
- TalkBack 초점 프로브4회 실패를 숨기지 않는다. 최초 알림 창·입력 주입·후속 GL 오류 중 원인 미확정. 안정적인 렌더링/실제 조작 환경에서 재현을 좁히고 음성을 확인해야 한다. 실제 NO_FILL도 미관찰이다.
- 개인정보 재열기 거부 선택은 자동 승인 검토가 법적 입력 경계라며 두 번 거부했다. 사용자 명시적 허용 질문은 응답 대기이며 우회하지 않았다. 해당 프로브는 중단/미완료다.
- 재개 순서: 승인된 테스트 거부 선택 또는 직접 조작 → 안정적인 기기의 TalkBack/NO_FILL 검증 → 실기기2대·실제3명·실제8h/30분/성능·SDK 배경 측정 → 최종 정보/키/계정 확보 후 정확한 AAB/업데이트/내부 테스트/정책 확인. RELEASE_READY 전체 AND 조건은 아직 충족하지 않았다.
- C:5.55GiB, 기존 AVD 정상 부팅 확인. 검사 설정 복구 및 작업 AVD 종료. 원격 미설정/push·공개·결제·실제 키 생성 없음.


### TalkBack 원인 진단 후속 완료

- 부모 요청으로 독립 진단을 계속해 shell input의 접근성 필터 우회를 원인으로 확인했다. 전용 AVD의 확인된 커널 키보드로 Alt+Right/Enter를 보내는 프로브로 수정했다. 제품 UI/경제/저장/광고 수정 없음. 소스·진단·승인 차단 원문 커밋 `2abe54f`.
- 최종 실제16KB TalkBack 홈/설정/도감1개(75.009초)와 같은 테스트 APK core35 통과. 빌드·unit23·lint0오류/각13경고, 새 테스트APK 서명/해시를 TEST-RESULTS에 기록했다. 이전 실패와 준비 조건 실패 로그는 삭제하지 않았다.
- 원인 미확정 접근성 실패를 사용자 준비 대기로 묶었던 요약은 정정한다. 이제 남은 접근성 검사는 실제 음성/발음·실기기 경험이며, 홈/설정/도감의 실제 초점·명칭·비활성 이유/잠김 설명은 통과다.
- 개인정보 선택 차단은 재시도/우회하지 않았다. 부모에게 CONSENT-APPROVAL-BLOCK의 명령/양식/거부 의미/오류 원문을 제공한다. 사용자 응답은 대기다.
- 실제 NO_FILL은 안전하게 반환시키는 승인된 서버/응답 미확보로 미검증. 코드상 공통 실패 처리와 실제 수신은 구분하며 ADS.md에 재개 조건 기록. 실기기2대·실제3명·8h/30분·성능·키/정책/계정/AAB 조건이 남아 RELEASE_READY 아님.
