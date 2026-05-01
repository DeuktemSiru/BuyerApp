# 득템시루 구매자 앱

마감 할인 상품을 둘러보고 픽업 주문을 예약하는 득템시루 구매자용 Android 앱입니다.

## 개요

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_buyer` |
| 플랫폼 | Android |
| 패키지 | `com.example.deuktemsiru_buyer` |
| 백엔드 기본 주소 | `http://10.0.2.2:8080/` |
| 인증 | 로그인 후 발급된 JWT를 `Authorization: Bearer {token}` 헤더로 전송 |

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 온보딩/인증 | 약관 동의 후 구매자 로그인 또는 회원가입을 진행합니다. |
| 홈 | 카테고리별 할인 매장 목록을 조회합니다. |
| 지도 | 지도형 화면에서 주변 매장을 탐색합니다. |
| 매장 상세 | 매장 정보, 할인 메뉴, 픽업 가능 시간을 확인합니다. |
| 주문/결제 | 메뉴 수량과 픽업 시간을 선택해 주문을 생성합니다. |
| 픽업 확인 | 주문 완료 후 픽업 코드와 주문 정보를 확인합니다. |
| 찜 목록 | 관심 매장을 저장하거나 해제합니다. |
| 주문 내역 | 이전 주문과 주문 상태를 조회합니다. |
| 마이페이지 | 로그인 사용자 정보와 구매자 알림을 확인합니다. |

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| 언어 | Kotlin 2.1.21 |
| UI | XML View, ViewBinding, Material Components 1.13.0 |
| 아키텍처 | Single Activity, Fragment, Navigation Component 2.9.0 |
| 네트워크 | Retrofit 2.11.0, Gson Converter, OkHttp Interceptor |
| 비동기 | Kotlin Coroutines Android 1.8.1 |
| Android | minSdk 29, targetSdk 36, compileSdk 36.1 |
| 빌드 | Gradle Kotlin DSL, Android Gradle Plugin 9.0.1 |

## 실행 방법

### 1. 백엔드 실행

앱은 Android Emulator에서 로컬 백엔드에 접근하기 위해 `http://10.0.2.2:8080/`을 사용합니다. 먼저 백엔드를 실행합니다.

```bash
cd ../deuktemsiru_backend
./gradlew bootRun
```

### 2. 앱 실행

Android Studio에서 `deuktemsiru_buyer` 프로젝트를 열고 `app` 실행 구성을 사용합니다. 터미널에서는 디버그 APK를 빌드할 수 있습니다.

```bash
./gradlew assembleDebug
```

### 3. 서버 주소 변경

실기기 또는 원격 서버에 연결할 때는 `RetrofitClient.kt`의 `BASE_URL`을 변경합니다.

```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

## 샘플 계정

| 역할 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 구매자 | `buyer@test.com` | `1234` |

## 프로젝트 구조

```text
app/src/main/java/com/example/deuktemsiru_buyer/
├── MainActivity.kt
├── data/           # 공통 모델, 세션 관리
├── network/        # Retrofit 클라이언트, API 인터페이스, DTO
└── ui/
    ├── onboarding/ # 온보딩, 로그인, 회원가입
    ├── home/       # 홈, 매장 목록
    ├── map/        # 지도형 탐색
    ├── detail/     # 매장 상세, 메뉴 목록
    ├── payment/    # 주문/결제
    ├── pickup/     # 픽업 확인
    ├── wishlist/   # 찜 목록
    ├── orders/     # 주문 내역
    └── mypage/     # 마이페이지
```

## 연동 API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 |
| `GET` | `/api/stores?category={category}&userId={userId}` | 매장 목록 조회 |
| `GET` | `/api/stores/{storeId}?userId={userId}` | 매장 상세 조회 |
| `POST` | `/api/orders?buyerId={buyerId}` | 주문 생성 |
| `GET` | `/api/orders?buyerId={buyerId}` | 내 주문 목록 조회 |
| `GET` | `/api/orders/{orderId}` | 주문 상세 조회 |
| `POST` | `/api/wishlist/{storeId}?userId={userId}` | 찜 토글 |
| `GET` | `/api/wishlist?userId={userId}` | 찜 목록 조회 |
| `GET` | `/api/users/{userId}` | 사용자 정보 조회 |
| `GET` | `/api/notifications?userId={userId}` | 구매자 알림 조회 |
