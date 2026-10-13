# 검증 결과

기준일: 2026-10-12 UTC. 최신 검증 소스 `2abe54f` (implementation/local-release). 아래 이전 검사와 산출물 표는 당시 `9e77ea4`의 이력이며, 최신 산출물은 마지막 절을 따른다. 미검증을 통과로 표시하지 않는다. 최종 판정은 RELEASE-READINESS.md.

## 실행한 검사

| 검사 | 결과 | 근거 |
|---|---|---|
| 순수 규칙·경제·저장·광고 게이트·수명 큐 | 23개 통과 | final-jvm-core.txt 및 verification/build/test-results/test/*.xml |
| Android 호스트 단위 테스트 | 23개 통과 | api26-final-suite.txt, app/build/test-results/testDebugUnitTest |
| API36 일반4KB | 31개 장치 테스트 통과 | api36-retest.txt, artifacts/qa-run/api36-green |
| 작은 화면+60dp 배너의 첫 강화 노출 | RED 실제실패 → GREEN | compact-home-red.txt / compact-home-green2.txt |
| API26 전체 | 32개 장치 테스트 통과 | api26-final-suite.txt, artifacts/qa-run/api26-green |
| API26 가로640×360dp+글자1.3 | 도감 상세 스크롤/설정 스위치/네이티브 로딩3개 통과 | api26-landscape-font13.txt |
| 412×892dp / 600×960dp+글자1.3 | UI7개씩 통과 | ui-412dp-installed.txt / ui-600dp-font13.txt |
| API36 실제16KB | 33개 통과, 실패/오류/건너뜀0; 두 네이티브 라이브러리 명시적 로딩 포함 | page-size16kb.txt=16384, final-16kb-and-build.txt, artifacts/qa-run/16kb-green |
| debug/benchmark APK, 테스트 APK | 빌드 성공, 최종 해시 아래 기록 | final-16kb-and-build.txt (6분30초, BUILD SUCCESSFUL) |
| lintDebug/lintRelease | 각각 오류0 / 경고13 | app/build/reports/lint-results-*.xml/html |
| release 입력 가드 | 기대한 차단 확인: 최종 applicationId 누락 | release-guard-final.txt; exit1, 출시 빌드 성공으로 처리하지 않음 |
| APK 서명·ZIP16KB | debug/benchmark 모두 apksigner/zipalign exit0 | debug/benchmark-signature-final.txt, debug/benchmark-zipalign-final.txt |
| 실제 OS force-stop/재실행 | 20회 통과, 강화1 유지 | force-stop20.txt / artifacts/qa-run/force-stop20.json |
| 실제 OS 배경/복귀 | 20회 통과, 설정/구매/정산 불변값 | background20.txt / artifacts/qa-run/background20.json |
| 실제 재부팅 | API26 강화1 유지 및81초×3별빛 정산 | api26-offline-purchase.json→api26-after-reboot.json |
| 이전 오프라인 시도 | 구매 저장은 확인했으나 전체 통신 차단 증거 불충분. OS-EDGE-RESULTS의 IPv4/IPv6 후속 결과로 대체 | api26-offline-routes.txt / api26-offline-ping.txt / api26-verified-offline-purchase.json |
| 실제 테스트 배너 | Google Test Ad 표시, 경계 분리 관찰 | artifacts/screens/growth-360.png, banner-360.png, banner-ui.xml |
| 실제 화면 | 홈/성장/도감/설정4장, 육안 확인 | docs/release/assets/screenshots |

표의 로그 파일은 모두 `artifacts/logs/` 아래에 있다. raw 로그/기기 데이터는 Git에 넣지 않는다. 공개 가능한 결과 요약과 실제 앱 화면만 문서에 보관한다. 호스트 단위 테스트의 DataStore 구현은 Windows 원자적 파일 교체를 위해 JVM artifact, 장치 테스트는 Android artifact다.

## 실패 이력과 수정

- 게임/저장/광고 게이트를 미구현 상태의 실패 테스트부터 구현했다. M1 RED6→GREEN6, M2 RED9실패→GREEN16, M3a RED2실패→GREEN20. 상세 progress.
- Activity 종료 중 취소 가능한 저장을 프로세스 소유 FIFO로 변경. 지연 저장/취소/다음 resume 순서 회귀 통과. 저장 성공 UI 원칙 유지.
- 실제 기기 SDK의 AgeRestrictedTreatment.valueOf 부재로 NoSuchMethodError 재현. 명시적 SDK 상수와 LinkageError 실패 처리로 수정, 실제 SDK 계약 테스트 통과.
- 최초 API36 실행은8개 결과2실패/나머지미완료. SDK 오류와 별도로 Windows QEMU0xc0000005 종료. Vulkan 비활성 후31개 통과했으나 수동 네트워크 검증 중 호스트가 다시 종료. 앱/에뮬레이터 충돌을 혼동하지 않는다.
- API26의 svc wifi/data 명령만으로는 가상 Wi-Fi가 단절되지 않아 이 첫 시도는 오프라인 통과로 취급하지 않았다. 전용 userdebug 에뮬레이터의 wlan0를 내려 실제 기본 경로 제거/Network is unreachable 확인 후 구매 성공을 확인했다. 검사 후 인터페이스·설정을 복구하고 에뮬레이터를 종료했다.
- 호스트 캐시·다운로드 정리와 사용자 공간 확보로 초기 부팅 용량 차단 해결. 일반API36 대신16KB 이미지를 순환 설치했다. 사용자 파일 삭제 없음.

## 네이티브·권한

APK에는 graphics-path와 DataStore 네이티브 라이브러리가 각각 arm64-v8a/armeabi-v7a/x86/x86_64로 포함된다. Kotlin 코드만 있다는 이유로 native 검사를 생략하지 않았다.

`tools/inspect-apk.py`로 ELF LOAD·RELRO 끝·ZIP offset을 읽고 SDK zipalign으로 검증한다. 기존 DataStore1.1.7 RELRO 정렬 문제를 공식 수정1.2.1로 해결했다. graphics-path1.0.1은 LOAD/ZIP16KB이지만 단순 RELRO 끝 검사에는 맞지 않았다. 공식 안정1.1.0도 같은 값이라 불필요한 업그레이드는 하지 않았다. 실제16KB 로딩 결과와 ARM64 실제 기기 검증은 구분한다.

최종 APK의 네이티브 파일 8개는 모두 ELF LOAD p_align≥16384, 비압축 ZIP offset16KB 정렬이다. `NativeLibrariesTest`가 API36 x86_64 PAGE_SIZE=16384에서 graphics-path와 datastore_shared_counter를 직접 로딩했고 전체 저장·UI 테스트33개도 통과했다. 이 결과는 ARM64 실기기 실행이나 정확한 출시 AAB 검증을 대신하지 않는다. RELRO 보조 검사 불일치는 잔여 관찰사항으로 유지한다.

공식 검사 근거: https://developer.android.com/guide/practices/page-sizes . DataStore 수정: https://android.googlesource.com/platform/frameworks/support/+/00bc339d026c39791f891ea9db2313c1fcdc97f9 . 상세 JSON: artifacts/logs/debug-native-final.json.

병합 권한: INTERNET, ACCESS_NETWORK_STATE, READ_BASIC_PHONE_STATE, Google AD_ID, WAKE_LOCK, FOREGROUND_SERVICE, 내부 DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. 게임 자체의 배경 서비스·wakelock 없음, 전경1Hz 타이머는 배경에서 취소. 전이 SDK의 WorkManager/Room 서비스는 포함되며 실제 배경 프로파일은 미검증. ADS.md 참고.

## 남은 검증

실기기2대 성능·메모리·30분 안정성·실제8h 이탈, 실제 TalkBack 음성·한국어 발음, 실제 NO_FILL, 재열린 개인정보 양식의 두 번째 거부/정상 닫기 승인·검증, 실제 업로드키/AAB 기반 설치·업데이트·Play 내부 테스트·보고서, 국가/연령/계정 조건은 미완료다. 등록 직후 종료·저장 장애·시계·UMP 필요/불필요/거부/오류·네트워크 복구의 후속 통과는 OS-EDGE-RESULTS에 기록했다. 에뮬레이터의 am start -W 수치를 실기기 p95 또는 첫 조작 가능 시간으로 사용하지 않는다.

## 산출물 식별

제품 소스 커밋 `9e77ea4`. 문서 커밋2b49884는 이 소스에서 문서·스토어 자산만 추가했다. 후속 인수인계 감사의 변경은 release 버전 입력 가드와 검증 도구에 한정되며 아래 APK는 다시 만들지 않았다. Gradle8.13/JDK21.0.10, versionCode1 / versionName0.1.0, minSdk26 / targetSdk36. 서명 AAB는 사용자 키/정보 미확정으로 생성하지 않았다.

| 산출물 (app/build/outputs/apk 아래) | 바이트 | SHA-256 |
|---|---:|---|
| debug/app-debug.apk | 22450869 | `19c21e51fa8b7e50440d344b3574cc1d0891f7b98970a28a653d8082ebbc39db` |
| androidTest/debug/app-debug-androidTest.apk | 1129540 | `ebfdd75d366ac4cc501aa8e8b9d2116e7315526e4a533e2a6038d87756259b81` |
| benchmark/app-benchmark.apk | 4482036 | `5a7248e919327a8a7c8d7bfd8396e9cddaeedf49441bc0ad163438dc6ff6511a` |

debug 패키지 `com.example.zerostar.debug`, benchmark 패키지 `com.example.zerostar.benchmark`. benchmark는 R8/리소스 축소·non-debuggable·profileable shell 설정이며 테스트 광고와 debug 서명을 사용한다. 배포용 키로 서명된 출시본이 아니다.

APK v2 debug 인증서 SHA-256: `eae79a024121d184d267bfebcd14ebba54b39df6b795ff09eacb7f6a5e84f6f3`. 키 원문/암호는 기록하지 않는다. 재현 명령은 [RELEASE-RUNBOOK](../release/RELEASE-RUNBOOK.md), 고정된 의존성 그래프는 [DEPENDENCIES](../release/DEPENDENCIES.txt).

추가 화면 검사 첫 adb 시도는 Gradle이 테스트 APK를 정리한 상태여서 runner를 찾지 못했다(ui-412dp.txt). 같은 최종 APK와 test APK 재설치 후412dp·600dp 각7개가 통과했다. 앱 동작 실패로 계산하지 않았으며 원래 오류 로그는 보존했다. 검사 뒤 화면 크기·density·font_scale을 복구했다.

추가 최적화 APK 실행: 최종 benchmark 해시의 APK를 실제16KB에 설치하고 MainActivity `Status: ok`·프로세스 유지·게임/테스트 배너 화면을 확인했다. `benchmark-launch16kb.txt`, `benchmark-package-flags.txt`, `benchmark-crash16kb.txt`, `artifacts/screens/benchmark-final16kb.png` 참조. debug 플래그 없음, min26/target36 확인. 단발 am start TotalTime1313ms는 p95/첫 조작 성능 합격 근거로 쓰지 않는다. 이때 화면에는 System UI 응답 없음 대화상자가 관찰됐고, lastanr 조회는 이번 부팅 기록 없음이었다. 원인을 확정하지 않았으며 `16kb-last-anr.txt`와 화면을 보존했다. 앱 프로세스 종료는 관찰하지 않았으나 30분 안정성 통과로 처리하지 않는다.

## 인수인계 감사 후 release 버전 가드

기존 가드는 버전 환경변수가 빠졌을 때 debug용 기본값1/0.1.0을 release에도 허용했다. 최종 버전 확정 요구에 맞춰 `ZEROSTAR_VERSION_CODE` 명시적 양수 및 `ZEROSTAR_VERSION_NAME` 비공백을 추가 검증한다. debug 기본값과 게임 런타임은 변경하지 않았다.

`python tools/check-release-guard.py`: 버전 코드 누락 / 버전 이름 누락 / 버전 입력 후 다음 광고 ID 가드 진입의3건. 수정 전2실패·1통과(`release-version-guard-red.txt`), 수정 후3통과(`release-version-guard-green.txt`). 모든 사례에서 실제 키·ID 없이 의도적으로 release 구성을 거부하며 AAB/APK는 생성하지 않는다. 테스트에서 개발 기본 버전을 허용한다고 사용자 버전 승인을 자동 획득한 것으로 해석하지 않는다.

원래 검증 APK3개의 해시·debug 서명·기기 결과는 위 표 그대로9e77ea4 소스 빌드에 귀속된다. 출시 서명 AAB는 계속 미생성이다.

후속 빌드 설정 검증: `:app:testDebugUnitTest :app:lintDebug :app:lintRelease` BUILD SUCCESSFUL(42초), 기존 단위23개·lint 각각 오류0/경고13 유지(`handoff-guard-checks.txt`). instrumentation 테스트 APK도 apksigner 확인 결과 APK Signature Scheme v2, Android Debug 인증서이며 앱 debug/benchmark와 같은 지문이다(`androidtest-signature-handoff.txt`). 배포용 업로드 키/Play App Signing 서명이 아니다.


## 검증 산출물 — d55d904 (후속 프로브 변경은 아래)

검증 소스 전체 해시: `d55d904888c72677df004735adc83b32910ecf3c`. 후속 문서 변경은 런타임/APK를 바꾸지 않는다. 버전1/0.1.0, min26/target36, 도구/서명 종류는 앞 절과 같다. debug APK는 제품 런타임이 불변이므로 기존9e77ea4 산출물과 동일 해시다. benchmark 및 테스트 APK는 아래 파일이 최신이다.

| 최신 산출물 | 바이트 | SHA-256 |
|---|---:|---|
| app/build/outputs/apk/debug/app-debug.apk | 22450869 | `19c21e51fa8b7e50440d344b3574cc1d0891f7b98970a28a653d8082ebbc39db` |
| app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | 1138047 | `28357e81d29fed68f38d56d2e13d4a638faf20dccdb3a68dad5e05b106ee0b26` |
| app/build/outputs/apk/benchmark/app-benchmark.apk | 4482036 | `bddd34ce51a133e4624b52c854125f5c8eeedcff33de920cbf8d0ed4e4b3a31d` |

| 최종 재검사 | 결과 | artifacts/logs 근거 |
|---|---|---|
| debug/test/benchmark 빌드·단위·린트 | BUILD SUCCESSFUL,46초. 단위23/실패0/오류0, lint debug/release 각각 오류0/경고13 | reviewed-final-build.txt 및 app/build XML |
| 실제16KB 최종 APK 기본 검사 | **35개 통과**,35.379초. PAGE_SIZE16384, 동일 최종 앱/테스트 APK 재설치 후 실행 | reviewed-core35-16kb.txt |
| 출시 버전 입력 가드 | 3/3 기대한 거부 통과, 출시 번들 생성 없음 | reviewed-release-guard.txt |
| APK3개 인증서 | Android Debug, v2 검증 성공. SHA256 지문 앞 절과 동일 | reviewed-debug/benchmark/test-signature.txt |
| debug/benchmark native 및 ZIP | native 각8개 ELF LOAD16KB 및 ZIP16KB 통과. RELRO 보조 한계는 앞 절 유지 | reviewed-debug/benchmark-native.json, *-zipalign.txt |
| 최종 benchmark 실행 | 실제16KB 설치/실행, 프로세스 유지, 복귀 안내·홈·Google Test Ad 화면 확인. crash buffer 비어 있음 | reviewed-benchmark-launch.txt, reviewed-benchmark-flags.txt, reviewed-crashes.txt; screens/reviewed-benchmark16kb*.png |

별도 opt-in 결과: 실제 UMP 불필요/필요/거부/통신오류는 API26에서 통과했다. 개인정보 재열기 프로브는 자동 승인 검토가 거부 선택을 차단해 중단했다. TalkBack 프로브는 터치2회·키보드2회에서 초점 탐색 조건 실패, 원인 미확정이다. **기본35개 통과를 이 두 별도 프로브 통과로 해석하지 않는다.** 실제 NO_FILL은 미관찰이다. 자세한 실패·환경·복구·판정 한계는 [OS-EDGE-RESULTS](OS-EDGE-RESULTS.md).

최종 정상 부팅에서는 접근성0/서비스null, 비행기 모드0, 자동시간1, font_scale1.0을 확인했다. host GPU에서 정상 앱·배너 화면을 다시 확인했지만 이전 그래픽 오류의 원인 해결이나 장기 안정성 통과를 의미하지 않는다. 마지막 C: 여유5.55GiB. 작업 에뮬레이터는 종료하며 API26/16KB 설치 파일은 보존한다.


## 최신 TalkBack 독립 진단 및 회귀 — 2abe54f

수정 범위는 androidTest의 AccessibilityServiceProbeTest와 진단 문서다. 제품 런타임·빌드 설정·의존성·경제·저장·광고 불변. debug/benchmark APK 해시는 위 d55d904 표와 동일하다. 새 테스트 APK는 1143297바이트, SHA-256 `abaa0ad1482d7994998a6e7ce55bf9650c6354c5b1d0711b3e6383d1854f5238`. Android Debug/v2 인증서 동일 검증 성공(talkback-final-test-signature.txt).

- 실제16KB에서 홈·설정·도감 TalkBack 프로브 **1개 통과, 75.009초**: talkback-final16kb.txt 및 talkback-final-labels.txt. 접근성 이름/금액/강화 비용·부족 이유, 설정2개, 도감 잠김 설명을 실제 노드에서 읽고 앱의 정확한 탭만 활성화했다. 광고 클릭 없음.
- 같은 최종 테스트 APK의 일반 core **35개 통과**: talkback-final-core35.txt. opt-in 실제 TalkBack1과 일반35를 구분한다.
- 빌드/검사: talkback-full-build.txt(1분11초, unit23/lint), talkback-final-build.txt(1분7초, 최종 테스트APK/lint). debug/release 각각 오류0/경고13. 제품 APK 재해시 동일.
- 기존4회 초점 실패의 직접 원인은 shell input이 접근성 InputFilter를 거치지 않는 검증 도구 입력 경로였다. 커널 키보드 경로에서 동일 앱의 실제 초점 이동을 확인하고 프로브를 수정했다. sysfs 읽기 권한·TalkBack bind·부팅 직후 준비 시간 실패도 따로 보존했다. [전체 원인과 RED/GREEN](TALKBACK-DIAGNOSIS.md).
- 실제 음성/한국어 발음·실기기 성능/실제8h 등은 이 무음 에뮬레이터 결과로 통과시키지 않는다. 과거 host GPU 오류가 완전히 해결됐다는 뜻도 아니다.
- 개인정보 양식 재검사는 부모 요청 이후 재시도/우회 없음. [CONSENT-APPROVAL-BLOCK](CONSENT-APPROVAL-BLOCK.md)에 실제 명령·대상·두 오류 원문·승인 질문을 기록했다. 검토자가 특정 승인 문구를 제시한 것은 아니다.


## 사용자 승인 후 개인정보 양식 검증 (2026-10-12 22:00~22:03 UTC)

제품/테스트 소스 및 APK는2abe54f와 동일, 코드·빌드 설정·의존성 변경 없음. 빌드/린트 재실행 대신 변경된 SDK 상태와 연결되는 회귀를 실행했다.

| 항목 | 결과 | artifacts 근거 |
|---|---|---|
| 현재 대상·버튼 재확인 | zerostar-16kb, emulator-5554, PAGE_SIZE16384, com.example.zerostar.debug/ComponentActivity, Publisher Test Ads의 Do not consent 확인 | screens/consent-approved-current.png |
| 승인 후 첫 거부 선택 | 실행 성공, SDK afterForm status3/ads=true/privacy=REQUIRED/error=null | logs/consent-approved-status.txt |
| 개인정보 선택 양식 재열기 | showPrivacyOptionsForm 호출 후 실제 양식 화면 확인 | screens/consent-approved-reopened.png, consent-approved-after-back.png |
| 재열린 양식 정상 닫기 | 미완료. 뒤로가기로 닫히지 않음. 두 번째 Do not consent 요청 자동 승인 거부 후 추가 시도 없음 | CONSENT-APPROVAL-BLOCK.md 최신 오류 원문 |
| 전체 SDK 프로브 | **실패1**,176.621초. 닫기120초 timeout, 이후 Activity 정리 때 SDK error3. 통과로 표시하지 않음 | logs/consent-approved-probe.txt 및 status.txt |
| 관련 회귀 | **6개 통과**,4.112초. AdsFlowTest4/AppLaunchTest1/개인정보 진입 UI1 | logs/consent-approved-regression.txt |
| 저장된 테스트 목적별 동의 | 회귀 이후 목적 동의11비트 모두0. 식별자·동의 문자열 원문 제외 | logs/consent-approved-purpose-summary.json |

동의 수락·실제 광고 클릭·계정/출시 변경·유료 지출 없음. 첫 거부와 재열기 증거가 확보됐으므로 이전의 ‘첫 거부부터 승인 차단’ 상태는 정정한다. 정상 닫기 콜백까지의 전체 검증은 여전히 미완료다. 재개 범위와 새로운 자동 검토 사유는 [CONSENT-APPROVAL-BLOCK](CONSENT-APPROVAL-BLOCK.md).

## 최신 재개 검사 및 공간 재확인 — 2026-10-13 UTC

변경은 ConsentSdkProbeTest의 기존 상태 보존 옵션/SDK 사전 로딩 대기와 문서뿐이다. debug/benchmark APK의 SHA-256은 기존 `19c21e51fa8b7e50440d344b3574cc1d0891f7b98970a28a653d8082ebbc39db` / `bddd34ce51a133e4624b52c854125f5c8eeedcff33de920cbf8d0ed4e4b3a31d` 그대로다. 새 테스트 APK:1143505바이트, SHA-256 `901710c130417b7ea1b09e8a8e416abab62adbffe01245099a4f2bcc6681fa8a`. Android Debug/v2 서명과 기존 인증서 SHA-256 동일 확인(privacy-close-test-signature.txt).

| 항목 | 결과 | 로컬 비추적 근거 |
|---|---|---|
| C: 공간·실행 | 최초5.16GiB, 기존 API36/16KB AVD 부팅26.975초 및 앱 실행 Status:ok. 종료 후5.29GiB. 공간 차단 없음 | disk-recheck-emulator.txt |
| 최초 상태 보존 재개 | 실패1,4.785초. 기존 OBTAINED 확인 후 사전 로딩 중 SDK error3. 선택 실행 없음 | privacy-close-probe.txt |
| 최종 빌드/unit/lint | assembleDebugAndroidTest/testDebugUnitTest/lintDebug/lintRelease 성공1분6초. unit23 실패0, lint각각 오류0/경고13 | privacy-close-final-build.txt 및 app/build 보고서 |
| 승인된 두 번째 거부/닫기 | 재개1개 통과55.484초. afterPrivacy error=null, 목적 동의11비트 모두0 | privacy-close-final-probe.txt/status.txt/purpose-summary.json |
| 최종 APK 일반 회귀 | core35 통과30.042초. SDK/TalkBack opt-in 제외 | privacy-close-core35.txt |
| 정상 게임 화면 | 앱 재실행 성공, 복귀 보상 확인 후 홈·테스트 배너 표시. crash buffer 비어 있음 | privacy-close-home.png/home-ready.png/crash.txt |

SDK 폼 진단과 실제 앱 재실행은 별도 검사다. 이전 첫 거부·재열기 후 타임아웃 실패를 소급해 통과시키지 않는다. 초기 명령의 `adb: unknown command am`은 shell 인자 누락으로 기기 실행 전 실패했으며 즉시 수정했다. 재부팅 직후 sdcard 캡처1회도 권한 오류로 실패해 /data/local/tmp에서 캡처했다. 제품 실패로 분류하거나 숨기지 않는다.

AVD 설정은 접근성0/비행기모드0/자동시간1/글자1.0 확인 후 종료했다. 실제 NO_FILL·음성/실기기 품질·3명·실제8h/30분·성능·키/정책/계정/AAB 조건은 미검증/대기다. 원격 push·공개·지출 없음. RELEASE_READY 판정은 여전히 미충족이다.
