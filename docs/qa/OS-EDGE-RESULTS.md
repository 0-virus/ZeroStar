# 독립 장치 검증 추가 기록

2026-10-12 UTC. 제품 런타임은 `9e77ea4`, 출시 버전 가드는 `7e4ddca` 이후 변경 없음. 아래는 자동 규칙 테스트와 별도로 실제 Android OS/UI를 조작한 결과다. 최종 APK 소스·해시는 TEST-RESULTS를 따른다. raw 로그와 기기 fixture는 Git에서 제외한다.

## 실행 결과

| 검사 | 환경 / 결과 | 근거 (artifacts 아래) |
|---|---|---|
| 등록 직후 강제 종료·재시작20회 | API26, 20/20 통과. 도감 C0~5를 순환 준비한 뒤 실제 등록 버튼/확인 UI로 저장하고 OS force-stop. 첫5회 초기화와 마지막 종료 상태·설정·중복 없음 확인 | logs/api26-registration-final.txt, qa-run/device-edges/registration.json |
| 손상·미래 스키마 | 실제 파일의 truncated JSON/schema2에 오류 UI, 재시도해도 원본 보존. 테스트 파일 복구 후 실제 재시도로 회복 | logs/api26-storage-edges-final.txt, qa-run/device-edges/storage.json |
| 쓰기 실패·명시 초기화 | game.json.tmp를 비어 있지 않은 테스트 디렉터리로 막음. 구매 실패 때 원본 유지, 장애 제거 후 재시도·구매 성공. 손상 초기화 취소는 보존, 확인은 기본 상태 | 같은 storage 결과 |
| OS 시각 +1h / -2h / +24h | API26 실제 date 변경. 각각3602초(실행 지연 포함),0초,28800초 지급. 재실행 중복 없음. auto_time 복구 | logs/api26-clock-edges-final.txt, qa-run/device-edges/clock.json |
| 실제 통신 차단 / 복구 | IPv4·IPv6 OUTPUT에서 wlan0/radio0 REJECT와 비행기 모드. UMP error2, ads=false. 게임 구매 성공·배너 숨김. 복구/Activity 재시작 후 구매 유지·테스트 배너1개 | logs/ump-offline-verified.txt, logs/api26-ad-lifecycle-hostgpu.txt |
| 배너 탭 전환20회·회전4회 | 설정의 최상위 광고 WebView0, 홈 최대1. 게임 상태 유지. 실제 광고 클릭 없음 | qa-run/ad-lifecycle/cycles.json 및 rotation 캡처 |
| 실제 UMP 지역·거부 | API26 Google 테스트 앱: 불필요 status1/ads=true; EEA status2/form=true/ads=false. Publisher Test Ads의 거부 선택 후 status3/ads=true, privacy=REQUIRED (SDK 결과이며 맞춤광고 동의 뜻 아님) | logs/ump-online-probe.txt, ump-eea-probe.txt, ump-regions-result.txt |
| 분할 화면 / 글자1.3 | API26 앱 영역360×327. 설정 토글 저장·도감 표시·홈 스크롤 후 강화1→2 통과 | logs/api26-split-stacks.txt, qa-run/split-actions.json, screens/api26-split-*.png |
| 접근성 의미 / 개인정보 진입 | 비활성 이유·비용·48dp·1Hz 수치가 live region 아님. 개인정보 필요 시 설정 버튼 노출/콜백, 불필요 시 숨김 | accessibility-semantics-api26.txt, core35-16kb-hostgpu.txt |

등록 검사의 완료 상태 fixture는 긴 생산 시간을 준비하는 용도다. 실제 OS 종료와 등록 UI는 실제 동작이지만, 20회 전체 경제 플레이를 실시간으로 했다는 뜻은 아니다. 시계 이동은 실제8시간 이탈 검사가 아니다.

## 환경 문제와 근거 정정

- 이전 wlan0 down / 기본 route 없음 / root ping 실패만으로 전체 오프라인을 증명했다는 해석을 철회한다. Android 정책 라우팅과 radio0 우회가 남을 수 있었고 실제 UMP 요청이 성공했다. 이번에는 두 인터페이스의 IPv4/IPv6 차단과 SDK 요청 실패를 함께 확인했다. 검사 후 작업용 방화벽 규칙·비행기 모드·시간·회전·글자 설정을 복구했다.
- API36 SwiftShader 및 swangle에서 QEMU0xc0000005 종료. API26 SwiftShader는 장기 검사 후 QEMU private 약11.28GB, Windows commit27.929/28.064GB로 ADB 응답 중단 및 Gradle native OOM을 유발했다. 앱 결함이나 디스크 부족으로 단정하지 않는다.
- 이 작업용 프로세스만 종료하고 **host GPU(Intel Iris Xe), Vulkan 끔, API26 RAM1024MB / API36 16KB RAM1536MB**로 변경했다. 이후 반복 광고/검사에서 QEMU private 약2.8~3.9GB. 빌드는 AVD 종료 상태에서 worker1/JVM1GB로 수행한다. ZeroVerse 프로세스/파일은 변경하지 않았다.
- 최초 저장 장애 도구는 빈 임시 디렉터리를 DataStore가 제거해 정리 명령이 실패했다. 비어 있지 않은 fixture로 고쳐 재검증했다. 최초 WebView 개수는 SDK 내부 중첩 노드까지 세었으며 최상위 광고 subtree 기준으로 수정했다. 이 도구 오류를 제품 수정으로 기록하지 않는다.
- 광고 전환 전후 PSS124328→139106KiB(+11.9%)는 즉시 관찰값이다. Activities1/AppContexts3/ViewRoot1은 유지됐지만 WebViews2→10, Views16→59로 변했다. 안정화·실기기 측정이 아니므로 메모리 합격이나 누수 없음으로 판정하지 않는다.

## 남은 검증과 재개 경계

실제8시간 이탈·실기기2대의30분/프레임/시작 속도/안정화 PSS·실제 음성 접근성은 미검증이다. 테스트 광고 서버의 실제 NO_FILL 응답도 관찰하지 못했으며 네트워크 실패로 대체 통과시키지 않는다. NO_FILL을 반환하는 승인된 테스트 환경/실제 응답을 확보해 게임·배너 숨김·복구를 확인한다. 임의 광고 ID나 추가 미디에이션을 넣지 않는다.

TalkBack16.0 서비스는16KB 이미지에 설치되어 있다. 처음 터치 주입 검사2회는 초점이 제목에 머물러 실패했다. 최초 알림 권한 창도 관찰했지만 이를 전체 원인으로 확정하지 않는다. 실제 음성 확인과 키보드 초점 결과는 최종 TEST-RESULTS에 별도 기록한다. [공식 TalkBack 탐색](https://support.google.com/accessibility/android/answer/6006598?hl=en)의 Alt+방향키 절차를 사용한다.

추가 키보드2회도 초점이 제목에 머물러 **실패**했다(`talkback-keyboard16kb*.txt`, `talkback-final-labels.txt`). 알림 권한을 거부한 뒤에도 같았다. 이후 OS 홈까지 검게 캡처되고 host GPU glReadPixels0x506/glPixelStorei0x501 및 시스템 com.google.android.as 크래시가 관찰됐다(`emulator16kb-final-error.txt`, `talkback-os-crash.txt`). TalkBack와 터치/키 입력 주입의 상호작용인지 제품 접근성 문제인지 확정하지 못했다. 이 검사를 통과나 단순 외부 대기로 처리하지 않는다. 재개 조건은 화면 렌더링이 안정적인 AVD/실기기에서 실제 탐색·음성 검사이며, opt-in 실패 프로브를 보존한다. 서비스/권한 실험 설정은 복구했다.

API36 테스트 동의 양식 거부 후 개인정보 양식 재열기 시도는 자동 승인 검토가 거부 버튼 선택도 법적 입력 경계일 수 있다고 차단했다. 재검토도 거부되어 우회하지 않았고, 사용자에게 테스트 거부 선택만의 명시적 허용을 요청했다. 이전 API26 거부 결과는 보존하지만, 개인정보 양식 재열기 전체 완료라고 표시하지 않는다. 설정 진입 콜백 테스트와 실제 SDK 양식 검사는 구분한다.
