# 자산 및 의존성 라이선스

2026-10-12. 자산은 로컬 번들. 웹 폰트·음원 CDN 요청 없음.

| 자산 | 출처 | 권리/고지 | 변경 |
|---|---|---|---|
| IBM Plex Sans KR Regular | https://github.com/google/fonts/tree/main/ofl/ibmplexsanskr | SIL OFL 1.1, app/src/main/assets/licenses/IBM-Plex-Sans-KR.txt 및 앱 설정 | 원본 TTF |
| Press Start 2P Regular | https://github.com/google/fonts/tree/main/ofl/pressstart2p | SIL OFL 1.1, app/src/main/assets/licenses/Press-Start-2P.txt 및 앱 설정 | 원본 TTF |
| 6단계·6종 픽셀 별 | ZeroStar PixelStar.kt 직접 작성 | 외부 이미지 복제 없음 | 팔레트·장식 자체 구현 |
| 강화·성장·도감 음향 | tools/generate-sounds.cjs 직접 합성 | 샘플 음원 사용 없음 | 22,050Hz mono PCM WAV |
| 디자인 토큰 | 로컬 ZeroVerse DESIGN-SYSTEM §1~5 및 CSS 읽기 참고 | 크림·잉크·산호·황혼 원칙 참조. 원본 코드/자산 수정 없음 | Android 재구성 |

AndroidX, Kotlin, kotlinx: Apache 2.0 계열. JUnit: EPL 1.0. Google Mobile Ads/UMP는 Google SDK 이용약관 적용. 정확한 전이 의존성 목록은 `:app:dependencies --configuration releaseRuntimeClasspath`로 추출해 최종 버전과 함께 재검토한다. 광고 SDK 전이 라이브러리의 데이터 처리도 개인정보 신고에 포함한다.

출시 전 최종 브랜드·디자인 사용 권리는 소유자가 확인해야 한다. 위 목록은 권리 법률 검토 완료를 의미하지 않는다.
