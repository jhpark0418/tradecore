# TradeCore
 
**거래 정합성과 동시성 제어에 집중한 Java 25 기반 Trading Core 프로젝트**
 
TradeCore는 암호화폐/증권형 주문 시스템에서 가장 까다로운 영역인 **주문 생성, 자산 lock/unlock, 체결 반영, 상태 전이, 멱등성, 동시성 충돌 처리**를 별도의 코어 프로젝트로 분리해 설계한 포트폴리오 프로젝트입니다.
 
기존의 실시간 시세/차트 프로젝트인 **Crypto Market Pipeline**에서 시장 데이터 흐름을 담당했다면, TradeCore는 그 위에서 실제 거래의 핵심이 되는 **Order / Account / Balance / Execution** 정합성을 담당합니다.
 
---
 
## 1. 프로젝트 개요
 
실제 거래 시스템에서는 단순히 주문을 저장하는 것보다 아래 문제가 훨씬 더 중요합니다.
 
- 동시에 들어온 주문/취소/체결 요청이 충돌할 때 어떻게 정합성을 지킬 것인가
- 동일 체결 요청이 재전송되었을 때 어떻게 멱등하게 처리할 것인가
- BUY/SELL에 따라 어떤 자산을 lock/unlock 해야 하는가
- 체결 시 주문 상태와 잔고 상태를 어떻게 함께 안전하게 변경할 것인가
 
TradeCore는 이 문제를 **도메인 모델 중심 구조 + 애플리케이션 서비스 + 버전 기반 충돌 감지**로 풀어내는 프로젝트입니다.
 
---
 
## 2. 프로젝트 목표
 
### 핵심 목표
- 거래 코어를 시장 데이터 처리 계층과 분리
- 주문/잔고/체결의 상태 정합성 보장
- 동시성 충돌을 명시적으로 다루는 구조 설계
- idempotency, optimistic locking, state transition을 코드 레벨에서 검증
- PostgreSQL + Testcontainers 기반 실제 DB 통합 테스트
 
### 포트폴리오 포인트
- 단순 CRUD가 아닌 **거래 도메인 문제 해결 능력** 강조
- **동시성 / 멱등성 / 상태 전이 / 정합성** 중심 설계 경험 표현
- 도메인 모델에서 실제 DB 영속성까지 일관된 낙관적 락 전략
- Java 기반 도메인 모델링과 테스트 설계 역량 표현
 
---
 
## 3. 기술 스택
 
| 구분 | 기술 |
|---|---|
| Language | Java 25 |
| Build | Gradle 9.4 (멀티모듈) |
| Framework | Spring Boot 4.0.4 |
| Persistence | Spring Data JPA, PostgreSQL |
| Test (Unit) | JUnit 5, Fake Repository |
| Test (Integration) | Testcontainers (PostgreSQL 17) |
 
### 모듈별 역할
 
| 모듈 | 역할 | 상태 |
|---|---|---|
| `tradecore-core` | 순수 도메인 + 애플리케이션 서비스 + 포트 인터페이스 | ✅ 구현 완료 |
| `tradecore-db` | JPA 엔티티, Repository Adapter, 통합 테스트 | ✅ 구현 완료 |
| `tradecore-api` | Spring Boot 진입점, HTTP 레이어 | 🔧 확장 예정 |
| `tradecore-infra` | Redis / Kafka 등 외부 연동 | 🔧 확장 예정 |
 
---
 
## 4. 프로젝트 구조
 
```text
tradecore
├─ tradecore-api      # Spring Boot 앱 진입점, API 레이어 확장 예정
├─ tradecore-core     # 핵심 도메인 및 유스케이스
├─ tradecore-db       # JPA 엔티티 + Repository Adapter + Testcontainers 통합 테스트
├─ tradecore-infra    # Redis/Kafka 등 인프라 모듈 (확장 예정)
└─ docs
   └─ concurrency-policy.md
```
 
### tradecore-core 주요 패키지
 
```text
core
├─ account      # Account, AccountId
├─ balance      # Asset, Balance
├─ market       # Symbol
├─ order        # Order, OrderId, OrderStatus, OrderType, OrderSide
├─ execution    # Execution, ExecutionId
└─ application
   ├─ order     # PlaceOrderService / CancelOrderService / ApplyExecutionService
   ├─ port.out  # AccountRepository, OrderRepository, ExecutionRepository 포트
   └─ exception # ConcurrencyConflictException
```
 
### tradecore-db 주요 패키지
 
```text
db
├─ adapter      # AccountRepositoryAdapter, OrderRepositoryAdapter, ExecutionRepositoryAdapter
├─ entity       # JPA 엔티티 (AccountEntity, OrderEntity, ExecutionEntity, AccountBalanceEntity)
└─ repository   # Spring Data JPA Repository 인터페이스
```
 
---
 
## 5. 현재 구현 범위
 
### 5-1. Account / Balance 도메인
 
TradeCore는 계정의 자산 상태를 `available` / `locked`로 분리하여 관리합니다.
 
```
Balance.lock(amount)              → available → locked
Balance.unlock(amount)            → locked → available
Balance.decreaseLocked(amount)    → 체결 시 잠긴 자산 차감
Balance.increaseAvailable(amount) → 체결 시 수령 자산 증가
```
 
이를 통해 주문 생성 시 자산을 잠그고, 취소 또는 체결 시 잠금 자산을 해제/정산하는 흐름을 명확하게 표현했습니다.
 
### 5-2. Order 도메인
 
주문은 다음 상태 전이를 갖습니다.
 
```
NEW → PARTIALLY_FILLED → FILLED
NEW → CANCELLED
PARTIALLY_FILLED → CANCELLED
```
 
구현 포인트:
- 지정가/시장가 주문 생성 팩토리 분리 (`Order.newLimitOrder()`, `Order.newMarketOrder()`)
- `cancel()`로 허용된 상태에서만 취소 가능
- `applyFill()`로 누적 체결 수량 반영
- `remainingQty()` 계산 지원
- 종결 주문(FILLED, CANCELLED)에 대한 추가 변경 차단
- `version` 필드 내장 — 낙관적 락의 기반
 
### 5-3. 주문 생성 유스케이스 (`PlaceOrderService`)
 
현재는 **LIMIT 주문만 지원**합니다.
 
- BUY 주문: quote asset 잠금 (`price × qty`)
- SELL 주문: base asset 잠금 (`qty`)
- lock 이후 Order 생성 및 저장
 
즉, 주문 생성 단계에서 이미 **실행 가능한 자산이 확보된 주문**만 시스템에 들어오도록 설계했습니다.
 
### 5-4. 주문 취소 유스케이스 (`CancelOrderService`)
 
- 주문 소유 계정 검증
- 미체결 잔량 기준 unlock amount 계산
- BUY는 quote asset, SELL은 base asset 반환
- 주문 상태를 `CANCELLED`로 전이
 
부분 체결 이후 남은 수량만큼만 자산이 해제되도록 설계했습니다.
 
### 5-5. 체결 반영 유스케이스 (`ApplyExecutionService`)
 
현재는 **LIMIT 주문 체결 반영**까지 구현되어 있습니다.
 
| 단계 | 처리 내용 |
|---|---|
| 중복 확인 | `executionId` 기준 기존 실행 여부 조회 |
| 멱등 처리 | 동일 내용이면 기존 결과 반환 |
| 충돌 감지 | 동일 ID + 다른 내용이면 `ConcurrencyConflictException` |
| BUY 체결 | locked quote 차감 + base 증가 + refund 처리 |
| SELL 체결 | locked base 차감 + quote 증가 |
| 상태 반영 | `filledQty`, `status` 동시 갱신 |
 
체결 반영은 단순 상태 변경이 아니라 **주문 상태 + 계정 잔고 + execution 기록**을 함께 갱신하는 핵심 유스케이스로 설계되어 있습니다.
 
---
 
## 6. 동시성 제어 전략
 
TradeCore는 도메인 모델부터 DB 계층까지 일관된 **Optimistic Locking** 전략을 적용합니다.
 
### 코어 레벨
- `Account`, `Order`는 각각 `version` 필드를 가짐
- Fake Repository에서 version 불일치 시 `ConcurrencyConflictException` 발생
 
### DB 레벨 (`tradecore-db`)
- `OrderEntity`, `AccountEntity`에 JPA `@Version` 적용
- 동시 수정 충돌 시 `ObjectOptimisticLockingFailureException` → `ConcurrencyConflictException` 변환
- Testcontainers 환경에서 실제 PostgreSQL로 충돌 시나리오 검증
 
```java
// OrderEntity.java
@Version
@Column(name = "version", nullable = false)
private Long version;
```
 
이 구조는 코어 도메인의 충돌 감지 정책이 DB 영속성 계층에서도 자연스럽게 연결됩니다.
 
---
 
## 7. 멱등성 처리 전략
 
체결 반영은 외부 시스템 재시도, 메시지 중복 전달, 네트워크 재전송이 자주 발생하는 영역입니다.  
`ApplyExecutionService`는 `executionId`를 기준으로 다음 규칙을 적용합니다.
 
```
같은 executionId + 같은 내용  → 기존 결과 반환 (멱등)
같은 executionId + 다른 내용  → ConcurrencyConflictException (충돌)
```
 
이 전략은 향후 Kafka consumer, outbox relay, 재처리 배치에서도 그대로 활용 가능한 구조입니다.
 
---
 
## 8. 테스트 구조
 
TradeCore는 두 가지 계층의 테스트를 갖습니다.
 
### Unit Test (`tradecore-core`)
 
외부 의존성 없이 순수 도메인 규칙을 검증합니다.
 
| 테스트 클래스 | 검증 내용 |
|---|---|
| `BalanceTest` | available/locked 잔고 계산, 경계 조건 |
| `AccountTest` | 자산 lock/unlock 정합성 |
| `OrderTest` | 상태 전이, applyFill, remainingQty |
| `SymbolTest` | 심볼 파싱 및 검증 |
| `PlaceOrderServiceTest` | BUY/SELL lock 자산 계산, 잔고 부족 |
| `CancelOrderServiceTest` | 부분 체결 후 잔량 기준 unlock |
| `ApplyExecutionServiceTest` | 멱등 처리, 충돌 감지, BUY/SELL 정산, refund |
| `FakeAccountRepositoryTest` | version 충돌 전파 검증 |
| `FakeOrderRepositoryTest` | version 충돌 전파 검증 |
 
### Integration Test (`tradecore-db`, Testcontainers)
 
실제 PostgreSQL(17)을 Testcontainers로 띄워 Repository Adapter를 검증합니다.
 
| 테스트 클래스 | 검증 내용 |
|---|---|
| `AccountRepositoryAdapterTest` | JPA 저장/조회, `@Version` 충돌 감지 |
| `OrderRepositoryAdapterTest` | JPA 저장/조회, `@Version` 충돌 감지 |
| `ExecutionRepositoryAdapterTest` | 체결 기록 저장/조회 |
 
포트폴리오 관점에서 이 프로젝트는 "기능이 된다"보다 **정합성이 깨지지 않는다**를 테스트로 증명하는 방향을 강조합니다.
 
---
 
## 9. 현재 API 상태
 
`tradecore-api` 모듈은 현재 Spring Boot 애플리케이션 진입점과 헬스 체크용 `PingController`만 존재합니다.
 
- `GET /api/ping` → `pong`
 
향후 추가 예정:
- 주문 생성 / 취소 / 조회 API
- 체결 반영 API
- 계정/잔고 조회 API
- idempotency key 기반 요청 처리
 
---
 
## 10. 설계 문서
 
### `docs/concurrency-policy.md`
 
코드 작성에 앞서 동시성 정책을 문서로 먼저 정의했습니다.
 
- 주문 취소와 체결의 충돌 정책
- 계정 잔고 lock/unlock 정합성 원칙
- executionId 기반 멱등성 규칙
- 충돌 발생 시 예외 처리 원칙
- version 전략과 DB 반영 방향
 
---
 
## 11. 실행 방법
 
### API 실행
```bash
./gradlew :tradecore-api:bootRun
```
 
### Core 테스트 실행
```bash
./gradlew :tradecore-core:test
```
 
### DB 통합 테스트 실행 (Docker 필요)
```bash
./gradlew :tradecore-db:test
```
 
### 전체 테스트 실행
```bash
./gradlew test
```
 
> DB 통합 테스트는 Testcontainers가 Docker 환경에서 PostgreSQL을 자동으로 실행합니다.
 
---
 
## 12. 이 프로젝트에서 강조하고 싶은 점
 
### 1) 거래 도메인에 맞는 모델링
이커머스식 상품/재고가 아닌, 거래 시스템에 맞는 개념인 `Symbol`, `Account`, `Balance`, `Order`, `Execution` 중심으로 모델링했습니다.
 
### 2) 상태 전이를 도메인에 내장
주문 상태 변경을 서비스 레이어에서 흩어 처리하지 않고, `Order` 도메인 내부 규칙으로 제한했습니다.
 
### 3) 도메인 → DB 일관된 낙관적 락 전략
코어 레벨의 `version` 기반 충돌 감지가 DB 계층의 JPA `@Version`으로 자연스럽게 이어집니다. Fake Repository에서 시작해 실제 PostgreSQL 통합 테스트까지 동일한 충돌 정책을 검증합니다.
 
### 4) 멱등성을 설계 1등 시민으로 취급
`executionId` 기반 멱등 처리는 "나중에 생각하자"가 아니라 초기 설계부터 반영했습니다.
 
### 5) 테스트 가능한 코어 우선 설계
실제 DB/인프라 의존 없이도 거래 핵심 규칙을 검증할 수 있도록 core 중심으로 먼저 설계했습니다.
 
---
 
## 13. 향후 확장 계획
 
### 단기
- 주문/체결/잔고 조회 API 추가
- account / order / execution 조회 모델 추가
 
### 중기
- Redis 기반 분산락 / idempotency 지원
- Outbox 패턴 도입
- Kafka 이벤트 발행
- 재처리/복구 전략 설계
 
### 장기
- 매칭 엔진 또는 execution consumer와 연동
- 리스크 체크 / 주문 제한 정책 확장
- 성능 테스트 및 장애 시나리오 검증
 
---
 
## 14. 한 줄 요약
 
**TradeCore는 "주문이 들어오면 저장한다" 수준이 아니라, 거래 시스템에서 가장 중요한 정합성과 동시성 문제를 Java 도메인 모델 → JPA 영속성까지 일관되게 설계하고 테스트로 증명한 Trading Core 프로젝트입니다.**