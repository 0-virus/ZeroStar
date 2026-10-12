# 광고·동의 검증

2026-10-12 UTC. 광고 통합 구현과 실제 Google Test Ad 표시·SDK 계약 검증 완료. UMP 필요/불필요/거부/정보 갱신 오류와 통신 복구를 추가 확인했다. 개인정보 재열기·실제 NO_FILL·실기기 성능·계정 설정은 미완료. 출시 준비 완료 아님.

- 고정 버전: Google Mobile Ads Next-Gen 1.5.0, UMP 4.0.0. 레거시 Ads·미디에이션·추가 분석 SDK 없음.
- debug/benchmark: Google 공식 배너 테스트 앱·단위 ID. release: 환경변수의 실제 ID 필수, 테스트 publisher ID를 빌드 가드에서 거부.
- Activity마다 UMP 정보 갱신, 필요 양식 표시. 갱신/양식 실패와 canRequestAds=false는 요청하지 않음. 필요 시 설정에 개인정보 선택 표시.
- 동의가 가능할 때 앱 컨텍스트로 IO dispatcher에서 프로세스당 1회 초기화. SDK 초기화 완료 이후 준비 상태 공개. 중복 콜백으로 재초기화하지 않음.
- 홈이 RESUMED일 때만 배너 1개. 반환 안내/등록/초기화 대화상자 중 숨김. 화면 폭에 맞춘 적응형 크기. 로드 실패 영역 숨김, 자동 재시도 루프 없음. 탭/배경/Activity 해제 시 AdView.destroy.
- 연령 기본값은 개발 전용 UNSPECIFIED, UMP under-age=false, 광고 콘텐츠 G. 출시 대상 연령에 맞는 정책 판단은 사용자 확정 후 적용. 아동 대상 판정이나 Families 충족으로 간주하지 않음.
- 광고 없는 경제 규칙은 동일. 서버 로그인·결제·수동 보상 광고 없음.

## 상태 테스트와 남은 실행

AdsFlowTest 4개는 미동의, 중복 콜백, 동의 철회·닫힘, 광고 상태와 독립적인 경제를 확인한다. 실제 네트워크 요청/SDK AdView 동작 증거가 아니다.

실기기에서 동의 필요/불필요/요청 불가/정보 갱신 오류/선택 변경, 테스트 배너 표시, no-fill/오프라인/네트워크 복구, 배경/회전/재생성 반복, 터치 겹침, 메모리를 각각 검증해야 한다. Debug UMP 지역/기기 설정이 필요하면 공식 절차로 로컬에서만 지정한다. 광고 식별자·기기 ID를 저장소에 넣지 않는다. 실제 광고는 클릭하지 않는다.

## 데이터와 신고

게임 파일은 기기 내부에 저장되고 자체 서버로 전송하지 않는다. 광고 SDK는 IP 기반 일반 위치, 앱 상호작용, 진단, 기기/계정 식별자 등 데이터를 처리할 수 있으므로 ‘데이터 수집 없음’으로 신고하지 않는다. UMP 선택과 연령에 따라 실제 처리·공유 범위가 달라진다. 개인정보처리방침 초안과 데이터 보안 답변 근거를 사용자의 실제 설정에 맞춰 확정해야 한다.

정확한 병합 권한 및 네이티브 라이브러리는 TEST-RESULTS.md의 최종 산출물 검사를 참조한다. SDK가 추가하는 권한까지 확인하고 실제 신고에 반영한다. 자체 알림/위치/외부 저장소/연락처 요청은 없다.

## 공식 참고

- https://developers.google.com/admob/android/next-gen/quick-start
- https://developers.google.com/admob/android/next-gen/banner
- https://developers.google.com/admob/android/next-gen/privacy
- https://developers.google.com/admob/android/next-gen/targeting
- https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure

실제 ID, AdMob 계정/앱 연결, 소유 확인·app-ads.txt·앱 준비 상태, 공개 정책 URL, 지역/연령, UMP 메시지 모두 미확인. 사업자 콘솔에서 실제 선행 조건이 충족돼야 한다. no-fill 자체를 출시 실패나 수익 보장으로 해석하지 않는다.

## 최종 병합 Manifest 확인

debug의 최종 병합 권한: INTERNET, ACCESS_NETWORK_STATE, READ_BASIC_PHONE_STATE, Google AD_ID, WAKE_LOCK, FOREGROUND_SERVICE, 앱 내부 DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. 자체 작성 Manifest는 인터넷/네트워크만 요청하며 나머지는 전이 SDK/AndroidX에서 병합된다. 게임이 직접 전화 정보·wakelock·foreground service를 사용하지 않더라도 광고 SDK의 실제 동작과 데이터 신고를 별도 확인해야 한다. 백그라운드 WorkManager SystemAlarmService/SystemJobService/SystemForegroundService 및 Room MultiInstanceInvalidationService도 전이 의존성으로 포함된다. 자체 서버 DB/Room 저장소를 구현했다는 뜻은 아니다. SDK가 요구하는 선언을 근거 없이 삭제하지 않았다.

관련 SDK 매니페스트와 merger report를 artifacts/logs 및 app/build/outputs/logs에서 확인할 수 있다. SDK 배경 동작/메모리/배터리는 아직 장치 검증 전이다.


## 후속 실제 SDK / OS 증거

[OS-EDGE-RESULTS](OS-EDGE-RESULTS.md)에 API26 SDK 지역 상태·테스트 거부·IPv4/IPv6 차단(error2, ads=false)·게임 구매·복구·배너 탭20회/회전4회 결과를 기록했다. 거부 후 canRequestAds=true는 SDK가 허용하는 광고 요청 상태이며 맞춤광고 동의로 해석하지 않는다. 실제 계정의 메시지·연령·국가 설정 검증은 별도다.

개인정보 선택 진입의 조건부 노출과 callback은 core35에서 통과했다. 실제 SDK 개인정보 양식 재열기는 테스트 거부 버튼도 법적 입력 경계라는 자동 승인 검토 거부 때문에 중단했다. 사용자의 테스트 거부 선택 명시적 허용/직접 조작 후 재개한다. 네트워크 실패를 실제 NO_FILL 응답으로 대체하지 않는다. 즉시 PSS 관찰과 안정화·실기기 누수 판정도 구분한다.

실제 NO_FILL을 안전하게 반환시키는 서버/승인된 테스트 응답은 확보하지 못했다. 공식 테스트 단위는 실제 검사에서 광고를 채웠으며, 통신 차단은 UMP 갱신 오류 단계에서도 요청을 막는다. BannerAd.onAdFailedToLoad는 오류 종류와 무관하게 loaded=false로 영역을 숨기지만, 이 코드 확인을 NO_FILL 콜백의 실제 수신으로 보고하지 않는다. 승인된 NO_FILL 테스트 응답/관찰 환경 확보 후 해당 콜백·게임 지속·영역 해제·복구를 검증한다. 임의 광고 ID/실제 광고 클릭/새 미디에이션으로 강제하지 않는다.
