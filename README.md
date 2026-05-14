# 득템시루 구매자 앱

마감 임박 할인 상품을 탐색하고 픽업 주문을 예약하는 **득템시루** 구매자용 Android 앱입니다. 카카오 로그인, 구글 지도 탐색, Tmap 경로 안내를 통해 주변 할인 매장을 빠르게 찾고 시루 잔액으로 결제할 수 있습니다.

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_buyer` |
| 플랫폼 | Android |
| 패키지 | `com.example.deuktemsiru_buyer` |
| 앱 버전 | 1.0 |
| minSdk | 29 (Android 10) |
| targetSdk | 36 |
| compileSdk | 36.1 |
| 백엔드 기본 주소 | `http://10.0.2.2:8080/` (에뮬레이터 → 로컬 PC) |
| 인증 방식 | `Authorization: Bearer {accessToken}` |

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 | Kotlin 2.1.21 |
| UI | XML View, ViewBinding, Material Components 1.13.0 |
| 아키텍처 | Single Activity + Fragment, Navigation Component 2.9.0 |
| 네트워크 | Retrofit 2.11.0, Gson Converter, OkHttp Interceptor |
| 비동기 | Kotlin Coroutines 1.8.1 |
| 지도 | Google Maps SDK 19.0.0 |
| 위치 | Google Play Services Location 21.3.0 |
| 경로 안내 | Tmap 보행자 경로 API |
| 소셜 로그인 | Kakao SDK User 2.21.0 |
| 빌드 | Gradle Kotlin DSL, Android Gradle Plugin 9.0.1 |

## 주요 기능

| 화면 | 설명 |
| --- | --- |
| 온보딩 | 시작 안내, 약관 동의, 시루 결제 연동 안내 |
| 카카오 로그인 | Kakao SDK로 로그인 후 백엔드 JWT 발급 |
| 홈 | 카테고리별(베이커리·카페·음식점·마트 등) 마감 할인 매장 목록 |
| 지도 탐색 | Google Maps 위에 현재 위치와 매장 마커 표시 |
| 경로 안내 | Tmap 보행자 경로로 거리·소요 시간 안내 |
| 매장 상세 | 매장 정보, 할인 상품, 남은 픽업 시간, 찜 토글 |
| 장바구니 | 상품 추가, 수량 조정, 예상 절감 금액 계산 |
| 주문 / 결제 | 시루 잔액 차감 방식의 픽업 주문 생성 |
| 픽업 확인 | 주문 완료 후 픽업 코드 및 주문 상세 확인 |
| 찜 목록 | 관심 매장 저장, 카테고리 필터 |
| 주문 내역 | 진행 중 / 완료된 주문 이력 조회 |
| 마이페이지 | 회원 정보, 등급, 수신 알림 목록 |

## 화면 흐름

```
온보딩 (약관 · 시루 연동)
└── 카카오 로그인
    └── 홈 (하단 탭)
        ├── 홈         ──▶ 매장 상세 ──▶ 장바구니 ──▶ 결제 ──▶ 픽업 확인
        ├── 지도       ──▶ 매장 상세 ──▶ 경로 안내
        ├── 찜 목록    ──▶ 매장 상세
        ├── 주문 내역  ──▶ 주문 상세
        └── 마이페이지 ──▶ 알림
```

## 시작하기

### 사전 준비

- Android Studio 최신 버전
- Kakao Developers 앱 등록 후 네이티브 앱 키 발급
- Google Cloud Console에서 Maps SDK for Android 활성화 후 API 키 발급
- Tmap Developers에서 API 키 발급

### 1. 백엔드 실행

에뮬레이터에서 로컬 PC 백엔드에 연결하려면 기본 주소(`10.0.2.2:8080`)를 사용합니다.

```bash
cd ../deuktemsiru_backend
./gradlew bootRun
```

### 2. API 키 설정

프로젝트 루트의 `local.properties`에 아래 항목을 추가합니다.

```properties
MAPS_API_KEY=your_google_maps_api_key
TMAP_API_KEY=your_tmap_api_key
KAKAO_NATIVE_APP_KEY=your_kakao_native_app_key
```

`local.properties`는 `.gitignore`에 포함되어 있으므로 키가 외부에 노출되지 않습니다.

### 3. (선택) 원격 백엔드 연결

실기기 테스트나 운영 서버에 연결할 때는 `local.properties`에 아래 항목을 추가하고 앱을 다시 빌드합니다.

```properties
BACKEND_BASE_URL=http://your-backend-host:8080/
```

### 4. 앱 빌드 및 실행

Android Studio에서 `app` 실행 구성을 실행하거나 터미널에서 빌드합니다.

```bash
./gradlew assembleDebug
```

릴리스 빌드 시 `local.properties`에 서명 정보를 추가합니다.

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

## 프로젝트 구조

```
app/src/main/java/com/example/deuktemsiru_buyer/
├── DeuktemsiruBuyerApp.kt       # Application 클래스, Kakao SDK 초기화
├── MainActivity.kt              # Single Activity, Navigation 호스트
├── data/
│   ├── SessionManager.kt        # Access / Refresh Token 로컬 저장
│   ├── CartManager.kt           # 로컬 장바구니 상태 관리
│   ├── Models.kt                # 화면 레이어 데이터 모델
│   └── StoreRepository.kt       # 로컬 데이터 접근
├── network/
│   ├── RetrofitClient.kt        # OkHttp + 인증 인터셉터 설정
│   ├── ApiService.kt            # Retrofit 인터페이스 (백엔드 API)
│   ├── ApiModels.kt             # 백엔드 요청 / 응답 DTO
│   ├── TmapClient.kt            # Tmap Retrofit 클라이언트
│   ├── TmapApiService.kt        # Tmap 경로 API 인터페이스
│   └── TmapModels.kt            # Tmap 응답 DTO
└── ui/
    ├── onboarding/              # 시작 화면, 약관, 로그인
    ├── home/                    # 홈, 카테고리, 매장 목록
    ├── map/                     # Google Maps 기반 매장 탐색
    ├── route/                   # Tmap 보행자 경로 안내
    ├── detail/                  # 매장 상세, 찜, 상품 담기
    ├── cart/                    # 장바구니
    ├── payment/                 # 주문 / 결제
    ├── pickup/                  # 픽업 코드 확인
    ├── wishlist/                # 찜 목록
    ├── orders/                  # 주문 내역
    └── mypage/                  # 마이페이지, 알림
```

## 연동 API

`RetrofitClient`는 `SessionManager`에서 읽은 Access Token을 모든 요청의 `Authorization` 헤더에 자동으로 첨부합니다. 401 응답 시 Refresh Token으로 토큰을 재발급합니다.

### 인증

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인 / 자동 회원가입 |
| `POST` | `/api/v1/auth/refresh` | Access Token 갱신 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `POST` | `/api/v1/auth/siru/link` | 시루 계정 연동 |

### 매장 / 찜

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/stores?category={category}` | 카테고리별 매장 목록 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 등록 / 해제 |
| `GET` | `/api/v1/wishlist` | 찜 목록 |

### 장바구니

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/cart` | 상품 추가 |
| `GET` | `/api/v1/cart` | 장바구니 조회 |
| `DELETE` | `/api/v1/cart/{cartItemId}` | 상품 삭제 |
| `DELETE` | `/api/v1/cart` | 장바구니 비우기 |

### 주문 / 사용자 / 알림

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders` | 주문 목록 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 |
| `GET` | `/api/v1/users/me` | 내 정보 조회 |
| `GET` | `/api/v1/notifications` | 알림 목록 |

## 외부 서비스

| 서비스 | 사용 목적 | 설정 키 |
| --- | --- | --- |
| Kakao Login | 소셜 로그인 | `KAKAO_NATIVE_APP_KEY` |
| Google Maps SDK | 지도 위 매장 마커 표시 | `MAPS_API_KEY` |
| Tmap 보행자 경로 | 매장까지 경로, 거리, 소요 시간 | `TMAP_API_KEY` |

## Android 권한

앱이 요청하는 주요 권한은 다음과 같습니다.

| 권한 | 용도 |
| --- | --- |
| `ACCESS_FINE_LOCATION` | 현재 위치 기반 매장 탐색 |
| `ACCESS_COARSE_LOCATION` | 대략적 위치 기반 탐색 |
| `INTERNET` | 백엔드 및 외부 API 통신 |
