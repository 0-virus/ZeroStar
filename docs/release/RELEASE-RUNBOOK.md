# 릴리스 재현 및 재개 절차

2026-10-12 UTC. 현재는 테스트 산출물만 제공한다. 실제 공개·원격 push·유료 지출·계정 동의는 수행하지 않았다. RELEASE_READY 및 릴리스 태그 없음.

## 도구와 검증

Windows PowerShell, JDK 21.0.10(소스/JVM target17), Gradle8.13 wrapper, AGP8.13.2, Kotlin2.2.21, Compose BOM2025.10.01. 앱 모듈 하나. min26 / compile36 / target36. SDK: platform36, build-tools35.0.0, platform-tools37.0.1. 버전 카탈로그 참조.

SDK는 현재 `.tools/android-sdk`, 로컬 Gradle 캐시는 `.tools/gradle-network`이고 모두 Git 비추적이다. 다른 컴퓨터에서는 공식 SDK를 설치한 후 해당 절대 경로를 `local.properties`의 sdk.dir에 설정한다. SDK 약관은 소유자가 직접 검토·동의한다. JDK/SDK가 준비되고 의존성을 다운로드할 네트워크가 필요하다.

```powershell
./gradlew.bat -g .tools/gradle-network :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest :app:assembleBenchmark --max-workers=1 --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g' '-Pkotlin.compiler.execution.strategy=in-process'
./gradlew.bat -g .tools/gradle-network :app:connectedDebugAndroidTest --max-workers=1 --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g' '-Pkotlin.compiler.execution.strategy=in-process'
```

두 번째 명령은 승인된 기기 또는 부팅된 에뮬레이터가 필수다. API26 32개, API36 일반31개, API36 실제16KB33개 통과. 별도 `verification` 빌드는 같은 순수 소스·저장소 테스트를 JVM에서 실행하는 검증 도구이며 제품 모듈이 아니다. Windows 호스트 테스트만 DataStore JVM artifact를 사용하고 장치 테스트는 Android artifact를 사용한다.

산출 경로:
- `app/build/outputs/apk/debug/app-debug.apk`: 테스트 ID, 임시 패키지 `.debug`, debug 서명.
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`: 기기 테스트.
- `app/build/outputs/apk/benchmark/app-benchmark.apk`: R8/리소스 축소, non-debuggable, profileable shell, 테스트 광고, debug 서명. 출시 AAB 대체품 아님.
- 릴리스 입력 확정 후 `app/build/outputs/bundle/release/app-release.aab` 및 `app/build/outputs/apk/release/app-release.apk`.

## 사용자 확정 및 보안 입력

필수 공개 설정: 최종 `ZEROSTAR_APPLICATION_ID`, `ZEROSTAR_VERSION_CODE`, `ZEROSTAR_VERSION_NAME`, `ZEROSTAR_PRIVACY_URL`, `ZEROSTAR_SUPPORT_EMAIL`, `ZEROSTAR_ADMOB_APP_ID`, `ZEROSTAR_ADMOB_BANNER_ID`, `ZEROSTAR_AD_AGE_TREATMENT`(CHILD/TEEN/UNSPECIFIED), `ZEROSTAR_UMP_UNDER_AGE`(true/false). 실제 출시 국가·대상 연령·게임 등급을 먼저 확정한다. 연령 플래그만으로 Families 등 정책 통과를 보장하지 않는다.

서명은 사용자 소유 키를 비추적 안전 경로에 보관하고 `ZEROSTAR_KEYSTORE`, `ZEROSTAR_STORE_PASSWORD`, `ZEROSTAR_KEY_ALIAS`, `ZEROSTAR_KEY_PASSWORD`를 비밀 입력으로 설정한다. 키/비밀번호를 채팅·커밋·명령 기록·로그에 붙여 넣지 않는다. 사용자 보관 및 복구 절차를 확인하기 전 서명 완료로 처리하지 않는다. 현재 생성·보관·연결 미실행.

환경변수 입력 후:

```powershell
./gradlew.bat -g .tools/gradle-network :app:verifyReleaseConfiguration :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease --max-workers=1 --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g' '-Pkotlin.compiler.execution.strategy=in-process'
Get-FileHash app/build/outputs/bundle/release/app-release.aab -Algorithm SHA256
```

가드는 필수 입력/샘플 ID/임시 패키지 잔존을 막는다. 실제 계정 소유, URL 내용·접근성, 서명 보관, 정책 준수까지 자동 인증하는 기능은 아니다.

## 정확한 AAB 검증

1. `jarsigner -verify -verbose -certs`로 AAB 서명을 확인하고, 키 비밀 없이 인증서 SHA-256 지문만 별도 기록한다. bundletool 공식 배포의 버전/해시를 고정한다.
2. bundletool validate/dump manifest로 최종 패키지, versionCode, min/target SDK, debuggable=false, 광고 ID·권한을 점검한다. bundletool build-apks에서 정확한 AAB와 같은 키를 사용하며 암호는 지원되는 안전한 입력 방식으로 제공한다.
3. APK set을 테스트 기기에 설치하고 핵심 기능을 검증한다. 같은 최종 패키지/키의 낮은 versionCode 테스트 빌드에서 진행을 만든 뒤 높은 versionCode로 업데이트해 전후 상태를 비교한다.
4. APK/AAB 내 `.so` 목록과 각 ABI/ELF 정렬·ZIP 정렬을 확인하고 실제 16KB 환경에서 실행한다. 네이티브 파일이 없더라도 실행 검증은 별도다.
5. Play 내부 테스트 업로드와 설치는 계정 접근 권한 확보 후 수행한다. 현재 위임 범위에서 접근 권한 없음. 사전 출시 보고서를 검토하고 제공 안 될 경우 이유·대체 검증을 기록한다.
6. 모든 증거가 통과하면 검증된 커밋에 릴리스 태그를 만들고 AAB 해시·인증서·버전·테스트 결과를 연결한다. 아직 태그를 만들지 않는다. 원격 push는 별도 위임된 원격에만 수행한다.

## 스토어·계정

STORE-LISTING.md, PRIVACY-POLICY.md는 소유자 확정 전 초안이다. 이미지 2장은 직접 생성한 스토어 자료이며 실제 앱 스크린샷으로 취급하지 않는다. 홈/성장/도감/설정 실제 debug 화면4장은 assets/screenshots에 확보했다. 최종 이름·정책·출시 AAB 확정 후 다시 캡처한다. 정책 URL은 공개 게시 권한 또는 사용자 제공 URL이 확보된 후 게시/접속/앱 내 열림을 검증한다.

로그인·유료 기능 없음 → 앱 액세스는 특별 계정 없이 접근하는 구조. 광고 포함 → 예. 데이터 보안은 광고 SDK 처리까지 반영. 대상 연령·콘텐츠 등급·출시 국가·운영자/연락처는 소유자 확정 필요. 앱 로컬 초기화는 광고 사업자가 보유한 데이터를 삭제하지 않는다. Play/AdMob 필수 인증과 약관은 사용자가 처리한다.

개인 계정에 적용되는 비공개 테스트 참여 인원·연속 기간·프로덕션 액세스 승인을 실제 Console에서 확인한다. 단순 경과 시간을 승인으로 처리하지 않는다. 공식 참고: 제품 명세 §10. 실제 제출일 정책을 다시 확인한다.

## 클라우드 재개

클라우드 컴퓨터에서도 SDK/JDK/Git을 갖추면 소스 빌드가 가능하지만 이 작업의 로컬 파일과 자동 공유되지 않는다. 현재 저장된 클라우드 코딩 환경/연결된 터미널 없음. 제공자의 CPU 아키텍처, 가상화/가속, 디스크, 기기 연결을 확인하기 전 에뮬레이터 실행 가능으로 확정하지 않는다. 이전이 선택되면 사용자가 승인한 비공개 소스 전달 방법으로 커밋을 전달하고 키·캐시·기기 데이터는 제외한다. 별도 전송/공개/비용 승인 없이 임의 이전하지 않는다. 실제 실기기 2대 요구는 클라우드 에뮬레이터로 대체하지 않는다.

## 현재 검증 산출물과 재개 지점

최종 검증 소스 커밋은 `2abe54f`다. 이후 문서 커밋은 앱 코드를 바꾸지 않는다. APK3개 SHA-256·크기와 debug 인증서 지문은 `docs/qa/TEST-RESULTS.md`, 실제 releaseRuntimeClasspath는 `DEPENDENCIES.txt`에 기록했다. release 입력 가드는 실제 실행에서 최종 applicationId 누락으로 기대한 실패를 반환했다. 이 실패를 서명 빌드 통과로 취급하지 않는다.

현재 보존된 AVD는 zerostar-16kb와 재설치한 zerostar-api26이다. 이미지는 각각 system-images;android-36;google_apis_ps16k;x86_64 및 system-images;android-26;google_apis;x86_64이다. 일반API36 이미지는 정리했다. 기존 테스트 근거는 `artifacts/logs`, `artifacts/qa-run`, 실제 캡처는 `docs/release/assets/screenshots`에 남아 있다.

PowerShell에서 ANDROID_HOME/ANDROID_SDK_ROOT를 `.tools/android-sdk` 절대 경로, ANDROID_AVD_HOME을 `.tools/avd` 절대 경로로 설정한다. 작업용 에뮬레이터는 `emulator.exe -avd zerostar-16kb -no-window -no-audio -no-boot-anim -no-snapshot -gpu host -feature -Vulkan -memory 1536 -cores 2`로 실행했다. Windows 백그라운드 실행은 Start-Process -WindowStyle Hidden을 사용한다. `adb shell getconf PAGE_SIZE`는16384여야 한다. 기기 선택과 충분한 디스크를 먼저 확인한다.

Gradle connected 검사는 종료 후 테스트 앱을 정리할 수 있다. adb instrumentation을 직접 재실행하려면 최종 app-debug.apk와 app-debug-androidTest.apk를 먼저 설치하고 `adb shell pm list instrumentation`의 실제 runner를 사용한다. 검사 종료 시 임시 화면 크기·density·font_scale을 복구한다.

후속 인수인계 감사에서 release 버전 입력 가드를 보완했다. 이제 ZEROSTAR_VERSION_CODE/NAME도 명시해야 한다. 회귀 검사는 python tools/check-release-guard.py로 실행한다. 후속 독립 검사 뒤 d55d904에서 APK를 다시 빌드했다. 최신 해시는 TEST-RESULTS 마지막 절을 따른다. 독립 추가 검증과 사용자 차단의 구분은 RELEASE-READINESS 마지막 절을 따른다.


## OS 경계 검사 / opt-in 진단

빌드와 AVD를 동시에 실행하지 않는다. 이 Windows 호스트에서 software GPU는 종료/commit 메모리 소진, host GPU는 TalkBack 검사 이후 그래픽 오류가 관찰됐다. host GPU는 core35와 API26 반복 검사에서 통과했지만 안정성이 보장된 환경은 아니다. 정상 화면/OS 상태를 확인하고 실패를 제품 통과로 치환하지 않는다.

최종 APK 두 개를 설치한 뒤 일반 core 검사35개:

```powershell
$adb='.tools/android-sdk/platform-tools/adb.exe'
& $adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s emulator-5554 shell am instrument -w -r -e notClass 'com.zerostar.game.ConsentSdkProbeTest,com.zerostar.game.AccessibilityServiceProbeTest' com.example.zerostar.debug.test/androidx.test.runner.AndroidJUnitRunner
python tools/check-device-edges.py --mode registration --count 20
python tools/check-device-edges.py --mode storage
python tools/check-device-edges.py --mode clock
python tools/check-ad-lifecycle.py
```

파이썬 도구는 전용 AVD 이름/qemu/debug 패키지를 검사하며 게임 테스트 데이터를 덮어쓴다. 개인 기기에 사용하지 않는다. 광고 도구는 zerostar-api26 전용이다. root 가능한 전용 이미지가 필요하고 시간·네트워크 설정을 복구한다.

ConsentSdkProbeTest는 probeGeography=EEA 또는 DISABLED, probeExpectError=true, probeShowForm=true, probeShowPrivacy=true를 명시하는 별도 SDK 진단이다. 실제 SDK가 로그에 안내한 테스트 기기 hash는 비추적 로컬 입력만 사용한다. 동의 선택은 사용자 직접 조작 또는 명시적 허용 경계에서만 수행한다. 2026-10-13에는 두 번째 거부 선택의 별도 허용 후 probePreserveConsent=true, probeGeography=EEA, probeShowPrivacy=true로 기존 상태를 보존해 정상 닫기까지 통과했다. probeShowForm은 생략해 첫 선택을 반복하지 않았다. 보존 모드는 OBTAINED/개인정보 옵션 REQUIRED를 확인하고, SDK 자동 사전 로딩을 10초 기다린다. 이후 로딩/표시 오류는 여전히 실패로 처리한다. 과거 타임아웃과 최초 사전 로딩 실패는 성공으로 바꾸지 않는다.

AccessibilityServiceProbeTest는 probeTalkBack=true로 명시하며, TalkBack을 켠 전용 AVD에서만 실행한다. 공식 Alt+Right를 root sendevent 커널 키보드 경로로 전달해 실제 accessibility focus를 읽는다. shell input은 접근성 필터를 건너뛰므로 이 검사의 대체 입력으로 사용하지 않는다. 기존 shell input 경로의 초점 실패는 원인을 찾아 수정했고, 홈/설정/도감 실제 초점 검사를 통과했다. `probeKeyboardDevice=/dev/input/event1`도 필요하며 `getevent -pl`의 실제 AT Translated Set 2 keyboard 경로를 먼저 확인한다. root 가능한 전용 qemu만 허용하고 sysfs 장치 이름을 확인한다. 기본 core에서 제외했다는 이유로 접근성 완료라고 보고하지 않는다. 실행 뒤 enabled_accessibility_services/accessibility_enabled를 기존 값으로 복구한다.


TalkBack 상세 RED/GREEN·준비 대기·fixture 근거는 ../qa/TALKBACK-DIAGNOSIS.md, 동의 차단의 명령/대상/오류 원문은 ../qa/CONSENT-APPROVAL-BLOCK.md를 따른다. 개인정보 선택 차단을 다른 입력 도구로 우회하지 않는다.
