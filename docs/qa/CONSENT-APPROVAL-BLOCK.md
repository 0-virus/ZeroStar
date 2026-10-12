# 테스트 동의 화면의 자동 승인 차단

2026-10-12 UTC. 부모가 사용자에게 확인할 수 있도록 실제 요청·대상·오류를 그대로 기록한다. 부모의 후속 지시에 따라 이 선택을 추가 재시도하거나 우회하지 않는다. 테스트 동의 수락은 수행하지 않는다.

## 차단된 대상과 동작

- 도구: functions.exec → tools.exec_command, PowerShell, sandbox_permissions=require_escalated.
- 전용 로컬 AVD: zerostar-16kb, adb serial emulator-5554, API36 Google APIs16KB x86_64. 개인 기기/실제 광고 계정이 아니다.
- 앱: com.example.zerostar.debug, 테스트 APK com.example.zerostar.debug.test. ConsentSdkProbeTest.requestDebugGeography가 ComponentActivity에서 공식 Google 테스트 앱 ID와 UMP4.0.0의 테스트 EEA 설정으로 연 양식.
- 화면 제목: **Welcome to Publisher Test Ads**. 본문: **Publisher Test Ads asks for your consent to use your personal data to:**. 표시 버튼은 **Consent**, **Do not consent**, **Manage options**.
- 요청한 선택: **Do not consent**. 개인화 광고·측정 등에 대한 이 테스트 양식의 동의를 거부하고 닫는 동작이며, Consent 수락은 아니다. 거부 후에도 SDK canRequestAds가 true일 수 있으므로 ‘광고 전체 차단’ 선택으로 설명하지 않는다.
- 확인한360×640 캡처에서 거부 버튼은 x35~325/y467~506, 요청 좌표는 중심(180,486). Consent 수락 버튼은 y413~451로 별도다. 원본 캡처: artifacts/screens/ump-privacy.png. 이 캡처를 본 뒤 클릭 요청했으나 자동 검토가 거부했다.
- 프로브는 첫 양식이 닫히면 showPrivacyOptionsForm을 호출할 예정이었다. **첫 거부 클릭에서 차단됐으므로 후속 개인정보 양식은 열리지 않았고, 그 양식의 버튼 이름/배치를 확인한 것으로 말하지 않는다.**

차단된 실제 클릭 명령:

```powershell
& .tools/android-sdk/platform-tools/adb.exe -s emulator-5554 shell input tap 180 486
```

첫 요청에는 위 명령 뒤 캡처/pull도 있었지만 도구 실행 자체가 거부됐다. 두 번째 요청은 위 한 줄만이고, justification에 방금 확인한 캡처·거부/수락 버튼 경계·테스트 거부 목적을 명시했다. 역시 거부됐다. 앱 프로세스 종료로 대기 프로브를 끝냈으며 성공으로 기록하지 않았다.

## 검토 오류 원문

첫 번째:

```text
This action was rejected due to unacceptable risk.
Reason: 동의/개인정보 화면의 좌표를 확인 없이 탭하므로 법적 동의를 실수로 수락하거나 설정을 변경할 수 있으며, 사용자는 직접 동의 입력을 위임하지 않았습니다.
Do not bypass this rejection through a workaround or indirect execution. Continue with a safer alternative, or carry out checks to prove that the action is authorized or low risk before trying again. Complete unaffected work without asking for confirmation. Report anything that remains blocked, clarify why it was blocked by auto-review, inform the user of the risk and ask for approval.
```

두 번째:

```text
This action was rejected due to unacceptable risk.
Reason: 좌표가 거부 버튼이라고 주장하는 근거는 untrusted 도구/assistant 설명뿐이며, 동의 UI에서 선택을 자동화하는 행위는 사용자의 직접 법적 입력 경계를 우회할 수 있습니다.
Do not bypass this rejection through a workaround or indirect execution. Continue with a safer alternative, or carry out checks to prove that the action is authorized or low risk before trying again. Complete unaffected work without asking for confirmation. Report anything that remains blocked, clarify why it was blocked by auto-review, inform the user of the risk and ask for approval.
```

검토자는 **정해진 승인 문구를 제시하지 않았다**. 위 오류가 정확한 요구이며, 다음 질문은 에이전트가 승인 범위를 좁혀 작성한 것이지 검토자가 지정한 인용문이 아니다.

> Google 테스트 동의 화면에서 ‘Do not consent(동의하지 않음)’를 선택하고, 이어 열리는 개인정보 선택 화면도 거부 상태로 닫는 검증을 허용하시나요? 자동 승인 검토가 테스트 화면의 거부 선택도 법적 입력 경계에 해당할 수 있다며 차단하여 이 동작만 명시적 승인이 필요합니다.

제시 선택지: ‘테스트 화면의 거부 선택만 허용’ / ‘직접 조작하겠음’ / ‘이번 동의 화면 검증 보류’. 이 질문의 답은 아직 받지 못했다. 부모가 사용자에게 확인한 뒤 범위를 전달하거나 사용자가 직접 조작해야 한다. 사용자의 일반 SDK 약관 ‘동의 완료’는 이 광고 양식 선택을 위임한 것으로 해석하지 않는다. 후속 양식의 내용은 실제 표시 시 다시 확인해야 하며 확인하지 않은 버튼을 자동 선택하지 않는다.
