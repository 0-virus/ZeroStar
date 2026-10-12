# TalkBack 초점 실패의 독립 진단

2026-10-12 UTC. 제품 코드를 바꾸기 전에 실패 입력 경로를 분리했다. 이전4회 실패는 실제 TalkBack 제스처/키 전달이 검증되지 않은 프로브 결과였다. 아래 실패 로그를 지우지 않는다.

## RED와 경계별 관찰

- `talkback-focus16kb.txt`, `talkback-focus16kb-retry.txt`: shell input swipe 반복에도 초점은 ZEROSTAR에 머묾.
- `talkback-keyboard16kb.txt`, `talkback-keyboard16kb-retry.txt`: shell input keycombination57/22로 바꿔도 동일. 서비스 enabled/bound는 확인했지만 ‘서비스 실행 중’이 ‘주입한 이벤트를 받음’을 뜻하지 않았다.
- 최초 알림 창 및 후속 host GPU 검은 화면은 실제 관찰이다. 그러나 새 정상 부팅·알림 거부 상태에서도 입력 문제를 분리해 확인했으므로 이를 TalkBack 실패의 확정 원인으로 사용하지 않는다.
- 실제 SDK 개인정보 선택 검사는 건드리지 않았다. 이 진단은 게임 복귀 안내·홈의 읽기 초점 탐색에 한정한다.

## 입력 경로 원인

Android InputDispatcher의 일반 입력 `notifyKey`/`notifyMotion`은 접근성 InputFilter를 호출한다. 반면 `injectInputEvent` 경로는 주입 이벤트를 큐에 넣으며 이 필터 호출을 거치지 않는다. shell input으로 앱에 이벤트를 보낼 수 있어도 TalkBack 탐색을 실행했다는 증거가 되지 않는다. [AOSP InputDispatcher 소스](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/main/services/inputflinger/dispatcher/InputDispatcher.cpp).

같은 정상 화면에서 일반 shell input 및 emulator console event send는 초점을 이동시키지 못했다. `getevent -pl`로 확인한 `/dev/input/event1`의 **AT Translated Set 2 keyboard**에 root sendevent로 Alt+Right 누름/해제를 전달하자 실제 TalkBack 초점이 복귀 제목에서 보상 문장으로 이동했다. TalkBack 서비스 dump의 inputMode도 -1→1로 바뀌었다. 캡처 `talkback-hardware-third.png`→`talkback-kernel.png`, 로그 `talkback-service-dump.txt`를 보존했다. [공식 Alt+방향키 탐색](https://support.google.com/accessibility/android/answer/6006598?hl=en)을 실제 입력 장치 경로로 실행한 비교다.

## 프로브 수정 범위

AccessibilityServiceProbeTest만 수정한다. 제품 UI/경제/저장/광고/의존성 변경 없음.

- `probeTalkBack=true`와 키보드 장치 경로를 명시해야 실행한다.
- qemu=1, userdebug root 사용 가능, 경로 형식, 실제 장치 이름을 검사한다. 개인 실기기를 대상으로 하지 않는다.
- UiAutomation은 DONT_SUPPRESS_ACCESSIBILITY_SERVICES로 TalkBack을 유지한다. 실제 커널 키 이벤트로 초점 이동 후 FOCUS_ACCESSIBILITY의 실제 명칭을 수집한다. 직접 focus 액션으로 결과를 만들어내지 않는다.
- 입력 장치 이름은 sysfs SELinux 권한 때문에 root 읽기가 필요했다. 최초 수정 실행은 장치 이름이 빈 값인 precondition에서 실패했다(`talkback-kernel16kb.txt`). root metadata 읽기로 고쳤으며 이 환경 검사 실패를 앱 결함으로 세지 않는다.
- 정상 입력 결과는 최종 검증 절에 기록한다. 화면 읽기의 실제 음성·한국어 발음·전 화면 사용자 경험은 이 무음 에뮬레이터 테스트만으로 통과시킬 수 없다.

## GREEN 및 준비 순서 회귀

- root 키보드 경로의 홈 탐색1개 **20.606초 통과**: `talkback-kernel16kb-bound.txt`, `talkback-kernel-home-labels.txt`. 이름→금액→생산량→별→성장→강화→부족 이유로 실제 초점 이동을 확인했다.
- 홈/설정/도감으로 확대한1개 **38.265초 통과**: `talkback-three-screens-warm.txt`, `talkback-three-screens-warm-labels.txt`. 강화의 합쳐진 자식 텍스트 ‘빛 모으기 · 20 별빛’과 enabled=false, 부족 이유, 효과음/움직임 줄이기, 도감0/6·별 이름·잠김 설명을 실제 accessibility node에서 확인했다. 설정/도감의 정확한 앱 패키지·탭 이름을 확인한 경우만 Alt+Enter로 열었으며 광고는 활성화하지 않았다.
- 부팅 직후 실행은 TalkBack bind 전의 precondition 실패(`talkback-kernel16kb-green.txt`)와 초기 초점 없음/늦은 이동 때문에 고정16회 안에 강화까지 도달하지 못한 실패(`talkback-three-screens16kb.txt`)가 있었다. 무조건 통과로 바꾸지 않고 준비 조건을 기다리며 탐색 횟수를 제한한다.
- 최종 프로브는 서비스 bind10초/첫 접근성 초점20초의 최대 대기, 홈 최대48회로 종료된다. 첫 강화20초가 지나는 속도에 의존하지 않도록 기존29회 구매액을 누적 생산으로 기록한 레벨29·잔액0의 유효 fixture를 사용한다. 다음 강화의 비용·생산 비율 때문에 부족 안내가 몇 분 유지된다. 게임 규칙/시계/생산률을 바꾸지 않는다.
- 최종 프로브 결과와 최신 테스트 APK 해시는 TEST-RESULTS 마지막 절에 연결한다. 이전4회 초점 실패를 사용자 기기 준비로 묶었던 판정을 정정한다. 직접 원인은 검증 도구의 입력 경로였으며 제품 UI 수정은 필요하지 않았다. 과거 host GPU 오류는 별도의 환경 이력으로 남긴다.

최종 준비 대기/레벨29 fixture 버전도 새 부팅의 실제16KB에서 **1개 통과,75.009초** (`talkback-final16kb.txt`, `talkback-final-labels.txt`). 홈·설정·도감 모두 확인했다. 원인 미확정 실패는 해결됐으며 음성 청취·실기기 품질 검증과 구분한다.
