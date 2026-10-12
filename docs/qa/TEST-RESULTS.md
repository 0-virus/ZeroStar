# 검증 결과

기준일: 2026-10-12 UTC. 제품 코드 커밋 `9e77ea4` (implementation/local-release). 미검증을 통과로 표시하지 않는다. 최종 판정은 RELEASE-READINESS.md.

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
| 실제 오프라인 진행 | 기본 경로 없음, ping Network is unreachable, 강화1→2 저장·누적-잔액45000milli | api26-offline-routes.txt / api26-offline-ping.txt / api26-verified-offline-purchase.json |
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

실기기2대 성능·메모리·30분 안정성·실제8h 이탈, TalkBack 음성/순서, 등록 직후 OS 종료 반복, 동의 필요/거부/정보갱신오류/선택변경의 실제 UMP 모든 분기, no-fill/네트워크복구 누수, 실제 업로드키/AAB 기반 설치·업데이트·Play 내부 테스트·보고서, 국가/연령/계정 조건은 미완료다. 에뮬레이터의 am start -W 수치를 실기기 p95 또는 첫 조작 가능 시간으로 사용하지 않는다.

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
