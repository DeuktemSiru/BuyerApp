<h1 align="center">득템시루 Buyer App</h1>

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/img_siheung_bi.png" width="180" alt="시흥시 BI"/>
  &nbsp;&nbsp;
  <img src="app/src/main/res/drawable-nodpi/img_siheung_character.png" width="96" alt="시흥시 캐릭터"/>
</p>

<p align="center">
  <b>마감 임박 할인 상품을 발견하고, 시루로 픽업 주문하세요</b><br/>
  주변 매장 탐색부터 장바구니, 주문, 픽업 QR, 길찾기까지 연결하는 구매자용 Android 앱
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Retrofit 2-48B983?style=for-the-badge&logo=square&logoColor=white" alt="Retrofit"/>
  <img src="https://img.shields.io/badge/Google Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white" alt="Google Maps"/>
  <img src="https://img.shields.io/badge/Kakao SDK-FFCD00?style=for-the-badge&logo=kakao&logoColor=black" alt="Kakao SDK"/>
</p>

---

## 프로젝트 소개

> **"오늘 팔리지 않으면 버려지는 상품을, 가까운 소비자에게 빠르게 연결합니다."**

**득템시루 Buyer App**은 시흥시 지역화폐 **시루**와 마감 할인 픽업 주문을 연결하는 구매자용 Android 앱입니다. 사용자는 주변 매장의 마감 임박 상품을 탐색하고, 장바구니에 담아 주문한 뒤, 픽업 코드와 QR 코드로 상품을 수령할 수 있습니다.

---

## 프로젝트 요약

| 항목 | 내용 |
| --- | --- |
| 프로젝트명 | `deuktemsiru_buyer` |
| 앱 역할 | 구매자 앱 |
| 플랫폼 | Android Native |
| 패키지 | `com.example.deuktemsiru_buyer` |
| 개발 언어 | Kotlin |
| UI 방식 | XML View + ViewBinding |
| 아키텍처 | Single Activity + Fragment + Repository |
| 백엔드 기본 주소 | `http://10.0.2.2:8080/` |
| 인증 방식 | JWT Bearer Token |
| minSdk / targetSdk | 29 / 36 |
| 버전 | 1.0 |

## 문제 정의

마감 시간이 가까운 상품은 아직 판매 가능한 상태여도 폐기되기 쉽고, 소비자는 주변 할인 상품을 실시간으로 찾기 어렵습니다. 득템시루 buyer 앱은 이 문제를 다음 흐름으로 해결합니다.

1. 현재 위치 기준으로 주변 할인 매장을 탐색합니다.
2. 매장 상세에서 마감 상품과 픽업 가능 시간을 확인합니다.
3. 장바구니와 주문 플로우를 통해 시루 잔액 기반 결제를 진행합니다.
4. 주문 승인 후 픽업 코드와 QR 코드로 현장 수령을 확인합니다.
5. 지도와 보행자 경로 안내로 매장까지 이동합니다.

## 주요 기능

| 기능 | 구현 내용 |
| --- | --- |
| 로그인 | Debug 로그인, 릴리스 환경 Kakao 로그인, 백엔드 JWT 저장 |
| 온보딩 | 약관 동의, 시루 계정 연동 안내 |
| 홈 | 카테고리, 검색어, 거리 기준 매장 목록 조회 |
| 지도 탐색 | Google Maps 기반 현재 위치 및 매장 마커 표시 |
| 매장 상세 | 상품 목록, 할인 정보, 픽업 마감 시간, 찜 토글 |
| 장바구니 | 서버 장바구니 동기화, 수량 변경, 삭제, 비우기 |
| 주문 / 결제 | 상품 주문 생성, 시루 잔액 기반 결제 UI |
| 픽업 | 주문 상태 폴링, 픽업 코드, QR 코드, 카운트다운 |
| 경로 안내 | Tmap 보행자 경로 API로 거리와 예상 시간 표시 |
| 찜 목록 | 관심 매장 조회 및 상세 이동 |
| 주문 내역 | 주문 목록, 주문 상세 BottomSheet, 취소 요청 |
| 마이페이지 | 회원 정보, 시루 잔액, 통계, 알림 설정 |

## 사용자 플로우

```text
온보딩
└─ 로그인
   └─ 홈
      ├─ 매장 상세
      │  ├─ 장바구니
      │  │  └─ 결제
      │  │     └─ 픽업 안내
      │  │        └─ 길찾기
      │  └─ 바로 주문
      ├─ 지도
      │  └─ 매장 상세
      ├─ 찜
      │  └─ 매장 상세
      ├─ 주문 내역
      │  └─ 주문 상세
      └─ 마이페이지
```

## 기술 스택

### Android

<p>
  <img src="https://img.shields.io/badge/Kotlin 2.1.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android Gradle Plugin 9.0.1-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android Gradle Plugin"/>
  <img src="https://img.shields.io/badge/XML View-3DDC84?style=flat-square&logo=android&logoColor=white" alt="XML View"/>
  <img src="https://img.shields.io/badge/ViewBinding-757575?style=flat-square&logo=android&logoColor=white" alt="ViewBinding"/>
  <img src="https://img.shields.io/badge/Material Components-757575?style=flat-square&logo=materialdesign&logoColor=white" alt="Material Components"/>
  <img src="https://img.shields.io/badge/Navigation Component-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Navigation Component"/>
</p>

### Network & Auth

<p>
  <img src="https://img.shields.io/badge/Retrofit 2.11.0-48B983?style=flat-square&logo=square&logoColor=white" alt="Retrofit"/>
  <img src="https://img.shields.io/badge/OkHttp Authenticator-000000?style=flat-square&logoColor=white" alt="OkHttp"/>
  <img src="https://img.shields.io/badge/Gson Converter-4285F4?style=flat-square&logo=google&logoColor=white" alt="Gson"/>
  <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
  <img src="https://img.shields.io/badge/EncryptedSharedPreferences-3DDC84?style=flat-square&logo=android&logoColor=white" alt="EncryptedSharedPreferences"/>
</p>

### External SDK

<p>
  <img src="https://img.shields.io/badge/Kakao SDK-FFCD00?style=flat-square&logo=kakao&logoColor=black" alt="Kakao SDK"/>
  <img src="https://img.shields.io/badge/Google Maps SDK-4285F4?style=flat-square&logo=googlemaps&logoColor=white" alt="Google Maps SDK"/>
  <img src="https://img.shields.io/badge/Tmap API-1428A0?style=flat-square&logoColor=white" alt="Tmap API"/>
  <img src="https://img.shields.io/badge/ZXing QR-000000?style=flat-square&logoColor=white" alt="ZXing QR"/>
</p>

## 구현 포인트

### 인증과 세션

- `SessionManager`가 회원 정보, Access Token, Refresh Token, 시루 연동 상태를 저장합니다.
- 가능한 경우 `EncryptedSharedPreferences`를 사용하고, 실패 시 일반 `SharedPreferences`로 fallback합니다.
- `RetrofitClient`는 모든 인증 요청에 `Authorization: Bearer {accessToken}` 헤더를 자동으로 추가합니다.
- 401 응답이 발생하면 OkHttp `Authenticator`가 Refresh Token으로 Access Token을 재발급하고 원 요청을 재시도합니다.
- Debug 빌드에서는 `POST /api/v1/auth/debug/login`을 사용해 빠르게 시연할 수 있습니다.

### 장바구니와 주문

- `CartManager`는 화면에서 필요한 장바구니 상태를 관리합니다.
- `CartRepository`는 서버 장바구니를 로드하고, 상품 추가/수량 변경/삭제/비우기를 백엔드와 동기화합니다.
- `OrderRepository`는 주문 생성, 주문 목록 조회, 주문 상세 조회를 담당합니다.
- 픽업 화면은 주문이 `CONFIRMED` 상태가 될 때까지 5초 간격으로 주문 상태를 폴링합니다.

### 지도와 길찾기

- `MapFragment`는 Google Maps로 현재 위치와 주변 매장을 표시합니다.
- `LocationHelper`가 위치 권한과 현재 위치 조회를 담당합니다.
- `RouteMapFragment`는 Tmap 보행자 경로 API를 호출해 매장까지의 경로, 거리, 소요 시간을 보여줍니다.

### 픽업 경험

- 주문 승인 후 픽업 코드를 문자 단위로 표시합니다.
- ZXing으로 픽업 QR 코드를 생성합니다.
- 픽업 마감까지 남은 시간을 카운트다운으로 보여줍니다.
- 픽업 코드 영역을 길게 누르면 코드가 클립보드에 복사됩니다.

## 프로젝트 구조

```text
app/src/main/java/com/example/deuktemsiru_buyer/
├── DeuktemsiruBuyerApp.kt       # Application, Kakao SDK 초기화
├── MainActivity.kt              # Single Activity, BottomNavigation, NavHost
├── data/
│   ├── CartManager.kt           # 화면 장바구니 상태
│   ├── CartRepository.kt        # 서버 장바구니 동기화
│   ├── Models.kt                # 화면 모델과 매핑 로직
│   ├── OrderRepository.kt       # 주문 API 접근
│   ├── SessionManager.kt        # 로그인 세션과 토큰 저장
│   └── StoreRepository.kt       # 매장 API 접근
├── network/
│   ├── ApiModels.kt             # 백엔드 DTO
│   ├── ApiService.kt            # Retrofit API 인터페이스
│   ├── RetrofitClient.kt        # Retrofit, OkHttp, 토큰 재발급
│   ├── TmapApiService.kt        # Tmap API 인터페이스
│   ├── TmapClient.kt            # Tmap Retrofit 클라이언트
│   └── TmapModels.kt            # Tmap 응답 DTO
├── ui/
│   ├── cart/                    # 장바구니
│   ├── detail/                  # 매장 상세
│   ├── home/                    # 홈, 매장 목록
│   ├── map/                     # 지도 탐색
│   ├── mypage/                  # 마이페이지
│   ├── onboarding/              # 온보딩, 약관, 시루 연동
│   ├── orders/                  # 주문 내역, 주문 상세 BottomSheet
│   ├── payment/                 # 결제
│   ├── pickup/                  # 픽업 코드와 QR
│   ├── route/                   # Tmap 길찾기
│   └── wishlist/                # 찜 목록
└── util/
    ├── CountdownFlows.kt        # 카운트다운 유틸
    ├── Extensions.kt            # 가격/시간/QR 포맷 유틸
    ├── LocationHelper.kt        # 위치 권한 및 현재 위치
    ├── MapViewLifecycleDelegate.kt
    └── Result.kt                # 성공/실패/로딩 sealed result
```

## 실행 방법

### 1. 사전 준비

- Android Studio 최신 안정 버전
- JDK 17 이상
- Android SDK 36
- Kakao Developers 네이티브 앱 키
- Google Cloud Console Maps SDK for Android API 키
- Tmap Developers API 키
- 로컬 또는 원격 `deuktemsiru_backend`

### 2. 백엔드 실행

에뮬레이터에서 로컬 PC의 Spring Boot 서버에 접근할 때는 `10.0.2.2`를 사용합니다.

```bash
cd ../deuktemsiru_backend
./gradlew bootRun
```

### 3. API 키 설정

프로젝트 루트에 `local.properties`를 만들거나 기존 파일에 아래 값을 추가합니다.

```properties
MAPS_API_KEY=your_google_maps_api_key
TMAP_API_KEY=your_tmap_api_key
KAKAO_NATIVE_APP_KEY=your_kakao_native_app_key
```

실기기나 원격 서버를 사용할 경우 백엔드 주소도 함께 설정합니다.

```properties
BACKEND_BASE_URL=http://your-backend-host:8080/
```

`local.properties`는 Git에 커밋하지 않습니다.

### 4. 앱 빌드

```bash
./gradlew assembleDebug
```

Android Studio에서는 `app` 실행 구성을 선택한 뒤 에뮬레이터 또는 실기기에서 실행합니다.

### 5. 릴리스 빌드

릴리스 빌드에서 `10.0.2.2` 기본 주소를 사용하면 앱이 실행되지 않도록 방어 로직이 들어 있습니다. 릴리스 테스트 전 `BACKEND_BASE_URL`을 반드시 설정합니다.

서명 빌드가 필요하면 프로젝트 루트에 `release.keystore`를 두고 `local.properties`에 아래 값을 추가합니다.

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

## 주요 API

### 인증

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/debug/login` | Debug 빌드용 로그인 |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인 / 회원가입 |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `POST` | `/api/v1/auth/siru/link` | 시루 계정 연동 |
| `DELETE` | `/api/v1/auth/siru/link` | 시루 계정 연동 해제 |

### 매장 / 찜

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/stores` | 위치, 반경, 카테고리, 검색어, 정렬 기준으로 매장 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 조회 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 등록 / 해제 |
| `GET` | `/api/v1/wishlist` | 찜 목록 조회 |

### 장바구니 / 주문

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/cart` | 장바구니 상품 추가 |
| `GET` | `/api/v1/cart` | 장바구니 조회 |
| `PATCH` | `/api/v1/cart/{cartItemId}` | 장바구니 수량 변경 |
| `DELETE` | `/api/v1/cart/{cartItemId}` | 장바구니 상품 삭제 |
| `DELETE` | `/api/v1/cart` | 장바구니 비우기 |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders` | 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 조회 |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | 주문 취소 |

### 회원 / 알림

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/members/me` | 내 정보 조회 |
| `GET` | `/api/v1/members/me/stats` | 구매 통계 조회 |
| `GET` | `/api/v1/members/me/notification-settings` | 알림 설정 조회 |
| `PUT` | `/api/v1/members/me/notification-settings` | 알림 설정 변경 |
| `GET` | `/api/v1/notifications` | 알림 목록 조회 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | 알림 읽음 처리 |
| `DELETE` | `/api/v1/notifications/{notificationId}` | 알림 삭제 |

## 외부 서비스 설정

| 서비스 | 사용 위치 | 설정 키 |
| --- | --- | --- |
| Kakao Login | 릴리스 로그인 | `KAKAO_NATIVE_APP_KEY` |
| Google Maps | 지도 탐색 | `MAPS_API_KEY` |
| Tmap Pedestrian Route | 픽업 길찾기 | `TMAP_API_KEY` |

## Android 권한

| 권한 | 용도 |
| --- | --- |
| `INTERNET` | 백엔드, Kakao, Google Maps, Tmap 통신 |
| `ACCESS_FINE_LOCATION` | 현재 위치 기반 매장 탐색 |
| `ACCESS_COARSE_LOCATION` | 대략 위치 기반 매장 탐색 |

## 시연 체크리스트

1. 백엔드 서버를 실행합니다.
2. `local.properties`에 지도, Tmap, Kakao 키를 설정합니다.
3. Debug 빌드에서 앱을 실행하고 `디버그 로그인으로 시작하기`를 누릅니다.
4. 홈에서 카테고리 또는 검색으로 매장을 찾습니다.
5. 매장 상세에서 상품을 장바구니에 담습니다.
6. 장바구니에서 수량을 변경하고 결제 화면으로 이동합니다.
7. 주문 생성 후 픽업 화면에서 승인 대기 상태를 확인합니다.
8. 주문이 승인되면 픽업 코드, QR 코드, 남은 시간을 확인합니다.
9. 길찾기 버튼으로 Tmap 보행자 경로 화면을 확인합니다.

## 트러블슈팅

| 상황 | 확인할 것 |
| --- | --- |
| 지도 화면이 비어 있음 | `MAPS_API_KEY` 값, Google Cloud Maps SDK for Android 활성화 여부 |
| 길찾기 실패 | `TMAP_API_KEY` 값, Tmap API 권한, 네트워크 연결 |
| 서버 연결 실패 | 백엔드 실행 여부, 에뮬레이터는 `10.0.2.2`, 실기기는 같은 네트워크의 PC IP 사용 |
| 릴리스 앱 실행 실패 | `BACKEND_BASE_URL`이 `10.0.2.2`가 아닌 실제 접근 가능한 주소인지 확인 |
| 로그인 실패 | Debug 빌드인지, Kakao 네이티브 키와 redirect scheme 설정이 맞는지 확인 |
| 위치가 표시되지 않음 | 앱 위치 권한 허용 여부, 에뮬레이터 위치 설정 |

## 학습 포인트

- Fragment 기반 Android 앱에서 Navigation Component로 화면 흐름을 구성했습니다.
- Retrofit과 OkHttp Authenticator로 토큰 인증과 자동 재발급을 구현했습니다.
- `EncryptedSharedPreferences`로 민감한 세션 정보를 저장했습니다.
- Google Maps와 기기 위치 권한을 결합해 위치 기반 서비스를 구현했습니다.
- 외부 Tmap API 응답을 앱 화면 모델로 변환해 경로 안내에 사용했습니다.
- 서버 장바구니와 로컬 UI 상태를 동기화하는 Repository 계층을 분리했습니다.
- 주문 승인 전후 상태를 폴링과 조건부 UI로 표현했습니다.