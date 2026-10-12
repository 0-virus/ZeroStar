# 테스트 동의 화면의 자동 승인 차단

2026-10-12 UTC. 부모가 사용자에게 확인할 수 있도록 실제 요청·대상·오류를 그대로 기록한다. 아래 최초 차단 이력 뒤의 **승인 후 재개 결과**가 최신 상태다. 테스트 동의 수락은 수행하지 않는다.

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

제시 선택지: ‘테스트 화면의 거부 선택만 허용’ / ‘직접 조작하겠음’ / ‘이번 동의 화면 검증 보류’. 최초 차단 당시에는 이 질문의 답을 아직 받지 못했다(이후 승인·재개 결과는 아래 참조). 부모가 사용자에게 확인한 뒤 범위를 전달하거나 사용자가 직접 조작해야 한다. 사용자의 일반 SDK 약관 ‘동의 완료’는 이 광고 양식 선택을 위임한 것으로 해석하지 않는다. 후속 양식의 내용은 실제 표시 시 다시 확인해야 하며 확인하지 않은 버튼을 자동 선택하지 않는다.

## 승인 후 재개 결과 (최신)

부모가 전달한 실제 사용자 응답은2026-10-12 21:53:19 UTC의 ‘승인’이며, 대상은 이 테스트 AVD의 Do not consent 선택·거부 후 동작·개인정보 양식 재열기다. 이어 중단된 작업을 다시 진행하라는 사용자 지시도 전달받았다. 첫 재개는 도구 실행 전 중단됐고 AVD/프로세스/새 로그/클릭 결과가 없음을 확인한 뒤 시작했다. 승인 증거를 도구 인수에 넣지 않았다.

1. 실제 연결이 zerostar-16kb / emulator-5554 / PAGE_SIZE16384이고 전경은 com.example.zerostar.debug의 ComponentActivity임을 확인했다. 새 캡처 `consent-approved-current.png`의 Publisher Test Ads에서 Do not consent 위치를 확인한 뒤 승인 후 첫 재시도1회가 실행됐다. 같은 좌표였지만 이전 캡처를 맹목적으로 재사용하지 않았다.
2. SDK EEA 정보:22:00:17 status2/form=true/ads=false/privacy=REQUIRED/error=null. 거부 선택 후22:01:09 `afterForm status=3 ads=true privacy=REQUIRED error=null`. Consent 수락이나 실제 광고 클릭 없음.
3. 프로브의 showPrivacyOptionsForm 호출로 개인정보 선택 양식이 다시 표시됐다. 새 캡처 `consent-approved-reopened.png`와 `consent-approved-after-back.png`에 같은 Publisher Test Ads 양식을 확인했다. 뒤로가기는 양식을 닫지 않았다.
4. 재열린 양식을 거부 상태로 닫으려 동일한 Do not consent 버튼을 새로 확인하고 요청했지만, 자동 검토가 이를 이미 실행한 거부 탭의 추가 재시도로 분류해 **거부**했다. 그 이후 추가 선택·재시도·우회 없음. 따라서 **첫 거부 처리·양식 재열기는 확인, 재열린 양식의 정상 닫기 완료는 미검증**이다.
5. 프로브는120초 닫기 대기 후 `Test privacy options dismissal timeout`으로 실패했다(전체176.621초). ActivityScenario 정리로 Activity가 소멸한 뒤 SDK가 `afterPrivacy ... error=3: Activity is destroyed.`를 반환했다. 이는 승인 차단 후 테스트 정리 결과이며 정상 닫기 성공이나 제품 런타임 결함으로 판정하지 않는다.
6. 관련 회귀6개(광고 게이트4, 앱 실행1, 조건부 개인정보 버튼/콜백1)는4.112초에 통과했다. 회귀 이후 SDK의 IABTCF_PurposeConsents11개 비트는 모두0임을 확인했으며 원문 식별자/동의 문자열은 문서에 넣지 않았다. canRequestAds=true를 개인화 동의 또는 전체 광고 금지로 해석하지 않는다.

새 거부 오류 원문:

```text
This action was rejected due to unacceptable risk.
Reason: 동일한 거부 탭이 이미 실행된 뒤 추가 재시도하는 동작으로, 확인된 사용자 승인 범위를 넘어 개인정보 양식 상태를 다시 변경할 수 있습니다.
Do not bypass this rejection through a workaround or indirect execution. Continue with a safer alternative, or carry out checks to prove that the action is authorized or low risk before trying again. Complete unaffected work without asking for confirmation. Report anything that remains blocked, clarify why it was blocked by auto-review, inform the user of the risk and ask for approval.
```

재개가 필요하면 사용자가 **‘재열린 개인정보 선택 양식에서 두 번째 Do not consent를 선택해 닫는 동작’**까지 명시적으로 승인하거나 직접 조작해야 한다. 이 문구는 필요한 범위를 설명한 것이며 검토자가 정해 준 승인 암구호가 아니다. 기존 첫 거부 승인을 다시 요구하는 것이 아니다. 현재 작업 AVD는 종료했다.

근거: artifacts/logs/consent-approved-probe.txt, consent-approved-status.txt, consent-approved-regression.txt, consent-approved-purpose-summary.json 및 artifacts/screens/consent-approved-*.png. 모두 로컬 비추적 검사 자료다.
