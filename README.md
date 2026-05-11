# 득템시루 구매자 앱

득템시루 구매자용 Android 앱입니다. 사용자는 카카오로 로그인한 뒤 주변 마감 할인 매장을 탐색하고, 상품을 장바구니에 담아 픽업 주문을 예약할 수 있습니다.

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_buyer` |
| 플랫폼 | Android |
| 패키지 | `com.example.deuktemsiru_buyer` |
| 앱 버전 | `1.0` |
| minSdk | 29 |
| targetSdk | 36 |
| compileSdk | 36.1 |
| 백엔드 기본 주소 | `http://10.0.2.2:8080/` |
| 인증 방식 | `Authorization: Bearer {accessToken}` |

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 온보딩/약관 | 시작 화면, 약관 동의, 시루 연동 안내 화면을 제공합니다. |
| 카카오 로그인 | Kakao SDK로 로그인하고 백엔드의 카카오 인증 API와 연동합니다. |
| 홈 | 카테고리별 마감 할인 매장 목록을 조회합니다. |
| 지도 탐색 | Google Maps에서 현재 위치와 매장 마커를 표시합니다. |
| 경로 안내 | Tmap 보행자 경로 API로 매장까지의 경로, 거리, 시간을 표시합니다. |
| 매장 상세 | 매장 정보, 할인 상품, 남은 시간, 찜 상태를 확인합니다. |
| 장바구니 | 같은 매장의 상품을 담고 수량, 선택 상태, 예상 절감 정보를 관리합니다. |
| 주문/결제 | 장바구니 또는 상품 상세에서 픽업 주문을 생성합니다. |
| 픽업 확인 | 주문 완료 후 픽업 코드와 주문 상세 정보를 확인합니다. |
| 찜 목록 | 관심 매장을 저장하고 카테고리별로 확인합니다. |
| 주문 내역 | 이전 주문과 주문 상태를 조회합니다. |
| 마이페이지 | 회원 정보, 등급, 알림을 확인합니다. |

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| 언어 | Kotlin 2.1.21 |
| UI | XML View, ViewBinding, Material Components 1.13.0 |
| 구조 | Single Activity, Fragment, Navigation Component 2.9.0 |
| 네트워크 | Retrofit 2.11.0, Gson Converter, OkHttp Interceptor |
| 비동기 | Kotlin Coroutines Android 1.8.1 |
| 지도/위치 | Google Maps 19.0.0, Play Services Location 21.3.0 |
| 소셜 로그인 | Kakao SDK User 2.21.0 |
| 빌드 | Gradle Kotlin DSL, Android Gradle Plugin 9.0.1 |

## 실행 방법

### 1. 백엔드 실행

Android Emulator에서 로컬 PC의 백엔드에 접근하기 위해 `10.0.2.2`를 사용합니다.

```bash
cd ../deuktemsiru_backend
./gradlew bootRun
```

### 2. 로컬 키 설정

프로젝트 루트의 `local.properties`에 필요한 키를 설정합니다.

```properties
MAPS_API_KEY=your_google_maps_api_key
TMAP_API_KEY=your_tmap_api_key
KAKAO_NATIVE_APP_KEY=your_kakao_native_app_key
```

릴리스 빌드를 만들 때는 같은 파일에 서명 정보도 추가합니다.

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 3. 앱 실행

Android Studio에서 `deuktemsiru_buyer` 프로젝트를 열고 `app` 실행 구성을 실행합니다. 터미널에서는 디버그 APK를 빌드할 수 있습니다.

```bash
./gradlew assembleDebug
```

### 4. 서버 주소 변경

실기기나 원격 서버에 연결하려면 `RetrofitClient.kt`의 `BASE_URL`을 변경합니다.

```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

## 프로젝트 구조

```text
app/src/main/java/com/example/deuktemsiru_buyer/
├── DeuktemsiruBuyerApp.kt
├── MainActivity.kt
├── data/           # 세션, 장바구니, 화면 모델
├── network/        # Retrofit, 백엔드 DTO, Tmap API
└── ui/
    ├── onboarding/ # 온보딩, 약관, 카카오 로그인
    ├── home/       # 홈, 카테고리, 매장 목록
    ├── map/        # Google Maps 기반 매장 탐색
    ├── route/      # Tmap 보행자 경로 안내
    ├── detail/     # 매장 상세, 찜, 상품 담기
    ├── cart/       # 장바구니
    ├── payment/    # 주문/결제
    ├── pickup/     # 픽업 코드 확인
    ├── wishlist/   # 찜 목록
    ├── orders/     # 주문 내역
    └── mypage/     # 마이페이지, 알림
```

## 연동 API

`RetrofitClient`는 로그인 후 저장된 Access Token을 모든 요청의 `Authorization` 헤더에 자동으로 첨부합니다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인/자동 회원가입 |
| `POST` | `/api/v1/auth/refresh` | Access Token 갱신 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `GET` | `/api/v1/stores?category={category}` | 매장 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 조회 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 등록/해제 |
| `GET` | `/api/v1/wishlist` | 찜 목록 조회 |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders` | 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 조회 |
| `GET` | `/api/v1/users/me` | 내 정보 조회 |
| `GET` | `/api/v1/notifications` | 알림 목록 조회 |

## 외부 연동

| 서비스 | 사용 위치 | 설정 |
| --- | --- | --- |
| Kakao Login | 온보딩 로그인 | `KAKAO_NATIVE_APP_KEY` |
| Google Maps | 지도, 경로 화면 | `MAPS_API_KEY` |
| Tmap 보행자 경로 | 장바구니 거리/탄소 계산, 경로 안내 | `TMAP_API_KEY` |
