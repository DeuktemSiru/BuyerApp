# 득템시루 Buyer App

마감 임박 할인 상품을 찾아 주문하고 픽업하는 구매자용 Android 앱입니다.

## 기술 스택

Kotlin · XML View / ViewBinding · Navigation Component · Retrofit · Kakao 로그인 · Google Maps · Tmap · ZXing

의존성과 Android SDK 설정은 [app/build.gradle.kts](app/build.gradle.kts)에서 관리합니다.

## 시작하기

### 사전 요구사항

- Android Studio, JDK 21, Android SDK 36.1
- Android 10(API 29) 이상의 기기 또는 에뮬레이터
- [Backend 시작하기](https://github.com/DeuktemSiru/Backend#시작하기)에 따라 별도 터미널에서 실행한 `dev` 서버

### 환경 설정

저장소 루트의 `local.properties`에 다음을 설정합니다.

```properties
MAPS_API_KEY=...
TMAP_API_KEY=...
KAKAO_NATIVE_APP_KEY=...
BACKEND_BASE_URL=http://10.0.2.2:8080/
```

실기기에서는 `BACKEND_BASE_URL`을 기기가 접근할 수 있는 서버 주소로 바꿉니다. 릴리스에는 HTTPS 주소가 필요합니다.
푸시를 사용하려면 `app/google-services.json`을 준비합니다.

릴리스 서명은 공통 [앱 릴리스 설정](https://github.com/DeuktemSiru/.github/blob/main/reference/deploy.md#앱-릴리스-설정)을 따릅니다. `local.properties`와 `google-services.json`은 커밋하지 않습니다.

### 빌드 및 설치

이 앱 저장소의 루트에서 실행합니다.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

`installDebug`에는 연결된 기기나 실행 중인 에뮬레이터가 필요합니다. Windows PowerShell에서는 `.\gradlew`를 사용합니다.

## 사용 방법

디버그 로그인 후 매장 탐색 → 장바구니 → 결제 → 판매자 승인 대기 → 픽업 코드·QR 확인 → 길찾기 순서로 시연합니다. 결제 범위와 주문 상태 규칙은 [요구사항](https://github.com/DeuktemSiru/.github/blob/main/spec.md)를 참고합니다.

### 트러블슈팅

| 증상 | 확인할 항목 |
| --- | --- |
| 지도·길찾기 실패 | Maps SDK 활성화, `MAPS_API_KEY`·`TMAP_API_KEY` |
| 서버 연결 실패 | 백엔드 실행 여부, 에뮬레이터 주소·실기기의 서버 접근 가능 여부 |
| 로그인 실패 | Debug 빌드 여부, Kakao 네이티브 키·redirect scheme |
| 릴리스 실행 실패 | HTTPS `BACKEND_BASE_URL`과 인증서 |

## 테스트

```bash
./gradlew testDebugUnitTest
```

자동 검증 설정은 [CI 워크플로](.github/workflows/ci.yml)를 참고합니다.

## 관련 문서

| 문서 | 내용 |
| --- | --- |
| [프로젝트 문서](https://github.com/DeuktemSiru/.github#관련-문서) | 요구사항·설계·작업 현황·배포 안내 |
| [Backend](https://github.com/DeuktemSiru/Backend) | 서버 실행·샘플 데이터 |
| [판매자 앱](https://github.com/DeuktemSiru/SellerApp) | 함께 사용하는 앱 |
