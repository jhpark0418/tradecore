# TradeCore

**거래 정합성과 동시성 제어에 집중한 Java 25 기반 Trading Core 프로젝트**

TradeCore는 암호화폐/증권형 주문 시스템에서 가장 까다로운 영역인 **주문 생성, 자산 lock/unlock, 체결 반영, 상태 전이, 멱등성, 동시성 충돌 처리**를 별도의 코어 모듈로 분리해 설계한 포트폴리오 프로젝트입니다.

---

## 1. 프로젝트 개요

실제 거래 시스템에서는 단순히 주문을 저장하는 것보다 아래 문제가 훨씬 더 중요합니다.

- 동시에 들어온 주문/취소/체결 요청이 충돌할 때 어떻게 정합성을 지킬 것인가
- 동일 체결 요청이 재전송되었을 때 어떻게 멱등하게 처리할 것인가
- BUY/SELL에 따라 어떤 자산을 lock/unlock 해야 하는가
- 체결 시 주문 상태와 잔고 상태를 어떻게 함께 안전하게 변경할 것인가

TradeCore는 이 문제를 **도메인 모델 중심 구조 + 애플리케이션 서비스 + 버전 기반 충돌 감지**로 풀어내는 프로젝트입니다.

---

## 2. 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 25 |
| Build | Gradle 9.4 (멀티모듈) |
| Framework | Spring Boot 4.0.4 |
| Persistence | Spring Data JPA, PostgreSQL 17 |
| Migration | Flyway |
| Messaging | Kafka, Spring for Apache Kafka |
| Infra | Docker, Docker Compose |
| Test (Unit) | JUnit 5, Fake Repository |
| Test (Integration) | Testcontainers (PostgreSQL 17), MockMvc |
| Test (Infra) | ApplicationContextRunner, Fake Publisher |

### 모듈별 역할

| 모듈 | 역할 | 상태 |
|---|---|---|
| `tradecore-core` | 순수 도메인 + 애플리케이션 서비스 + 포트 인터페이스 | 구현 완료 |
| `tradecore-db` | JPA 엔티티, Repository Adapter, 통합 테스트 | 구현 완료 |
| `tradecore-api` | Spring Boot 진입점, REST API 레이어 | 구현 완료 |
| `tradecore-infra` | Outbox Relay, Kafka Publisher, topic 설정 등 외부 연동 | Outbox Relay 1차 구현 완료 |

---

## 3. 프로젝트 구조

```text
tradecore
├─ tradecore-api      # Spring Boot 앱 진입점, REST API 레이어
├─ tradecore-core     # 핵심 도메인 및 유스케이스
├─ tradecore-db       # JPA 엔티티 + Repository Adapter + Testcontainers 통합 테스트
├─ tradecore-infra    # Outbox Relay + Kafka 발행 인프라
└─ docs
   └─ concurrency-policy.md
```

### tradecore-core 주요 패키지

```text
core
├─ account      # Account, AccountId
├─ balance      # Asset (BTC·ETH·USDT), Balance
├─ market       # Symbol
├─ order        # Order, OrderId, OrderStatus, OrderType, OrderSide
├─ execution    # Execution, ExecutionId
└─ application
   ├─ order     # PlaceOrderService / CancelOrderService / ApplyExecutionService
   ├─ query     # OrderSummary, OrderSearchCondition, PageResult
   ├─ port.out  # AccountRepository, OrderRepository, ExecutionRepository, OrderQueryRepository 포트
   └─ exception # ConcurrencyConflictException, ResourceNotFoundException
```

### tradecore-db 주요 패키지

```text
db
├─ adapter      # AccountRepositoryAdapter, OrderRepositoryAdapter,
│               # ExecutionRepositoryAdapter, OrderQueryRepositoryAdapter
├─ entity       # AccountEntity, AccountBalanceEntity, OrderEntity, ExecutionEntity
└─ repository   # Spring Data JPA Repository 인터페이스
```

### tradecore-api 주요 패키지

```text
api
├─ account      # AccountQueryController (계정 조회 + 주문 목록 조회)
├─ order        # OrderCommandController, OrderQueryController
├─ execution    # ExecutionCommandController
├─ common       # GlobalExceptionHandler, PageResponse
├─ bootstrap    # DemoDataInitializer
├─ application  # TradingCommandFacade, TradingQueryService
└─ config       # TradeCoreUseCaseConfig
```

### tradecore-infra 주요 패키지

```text
infra
└─ outbox
   ├─ OutboxRelay                 # PENDING outbox 이벤트 조회 및 발행 스케줄러
   ├─ OutboxEventPublisher         # 이벤트 발행 추상화
   ├─ KafkaOutboxEventPublisher    # KafkaTemplate 기반 이벤트 발행 구현체
   ├─ OutboxTopicResolver          # eventType → Kafka topic 매핑
   └─ OutboxTopicsProperties       # topic 이름 설정 바인딩
```

---

## 4. 현재 구현 범위

### 4-1. Account / Balance 도메인

TradeCore는 계정의 자산 상태를 `available` / `locked`로 분리하여 관리합니다.

```
Balance.lock(amount)              → available → locked
Balance.unlock(amount)            → locked → available
Balance.decreaseLocked(amount)    → 체결 시 잠긴 자산 차감
Balance.increaseAvailable(amount) → 체결 시 수령 자산 증가
```

이를 통해 주문 생성 시 자산을 잠그고, 취소 또는 체결 시 잠금 자산을 해제/정산하는 흐름을 명확하게 표현했습니다.

### 4-2. Order 도메인

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

### 4-3. 주문 생성 유스케이스 (`PlaceOrderService`)

현재는 **LIMIT 주문만 지원**합니다.

- BUY 주문: quote asset 잠금 (`price × qty`)
- SELL 주문: base asset 잠금 (`qty`)
- lock 이후 Order 생성 및 저장

즉, 주문 생성 단계에서 이미 **실행 가능한 자산이 확보된 주문**만 시스템에 들어오도록 설계했습니다.

### 4-4. 주문 취소 유스케이스 (`CancelOrderService`)

- 주문 소유 계정 검증
- 미체결 잔량 기준 unlock amount 계산
- BUY는 quote asset, SELL은 base asset 반환
- 주문 상태를 `CANCELLED`로 전이

부분 체결 이후 남은 수량만큼만 자산이 해제되도록 설계했습니다.

### 4-5. 체결 반영 유스케이스 (`ApplyExecutionService`)

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

### 4-6. 주문 목록 / 관리자 주문 검색 (`TradingQueryService` + `OrderQueryRepositoryAdapter`)

Command/Query를 명확히 분리하여 조회 전용 서비스(`TradingQueryService`)와 조회 전용 포트(`OrderQueryRepository`)를 별도로 운영합니다.

현재 조회 API는 두 가지 흐름을 지원합니다.

- 계정별 주문 목록 조회: `/api/accounts/{accountId}/orders`
- 관리자 전체 주문 검색: `/api/admin/orders`

구현 포인트:
- 계정별 조회는 `accountId` 조건을 필수로 적용
- 관리자 조회는 `accountId = null` 조건으로 전체 주문 검색
- `symbol`, `status`, `side`, `createdFrom`, `createdTo` 조건 필터링
- `createdAt desc`, `orderId desc` 기준 정렬
- 페이지네이션 (`page` / `size`) 지원 (최대 size 100)
- JPQL 동적 쿼리로 구현 (`OrderQueryRepositoryAdapter`)
- 응답에 `totalElements`, `totalPages`, `hasNext` 포함

관리자 조회에서도 계정 조건 없이 전체 주문을 대상으로 필터가 적용되도록 DB 통합 테스트로 검증했습니다.

---

### 4-7. Outbox 저장 + Kafka Relay 발행 구조

주문 생성, 주문 취소, 체결 반영 유스케이스에서 도메인 상태 변경과 함께 Outbox 이벤트를 저장하고, 별도의 Relay가 `PENDING` 이벤트를 Kafka로 발행합니다.

현재 구현된 이벤트 저장 흐름:

| 유스케이스 | 저장 eventType | Kafka topic |
|---|---|---|
| 주문 생성 | `ORDER_PLACED` | `tradecore.order.placed` |
| 주문 취소 | `ORDER_CANCELLED` | `tradecore.order.cancelled` |
| 체결 반영 | `EXECUTION_APPLIED` | `tradecore.execution.applied` |

```text
Command API
 → Core UseCase
 → DB 상태 변경
 → outbox_event 저장
 → OutboxRelay가 PENDING 이벤트 조회
 → Kafka 발행
 → 성공 시 PUBLISHED 상태 변경
 → 실패 시 FAILED/PENDING 재처리 대상 기록
```

구현 포인트:

OutboxEvent 도메인 모델 추가
OutboxStatus 상태값 관리 (PENDING, PUBLISHED, FAILED)
OutboxEventRepository 포트 추가
FakeOutboxEventRepository로 core 단위 테스트 지원
outbox_event 테이블 추가
OutboxEventRepositoryAdapter로 JPA 저장/조회 구현
findPending(limit), markPublished(), markFailed() 지원
OutboxRelay가 스케줄 기반으로 PENDING 이벤트를 batch 조회
OutboxTopicResolver가 eventType을 Kafka topic으로 변환
OutboxTopicsProperties로 topic 이름을 설정 파일에서 관리
KafkaOutboxEventPublisher가 KafkaTemplate<String, String> 기반으로 메시지 발행
tradecore.outbox.relay.enabled 설정으로 Relay 활성화/비활성화 가능

로컬 Kafka 환경에서 아래 세 이벤트가 모두 topic으로 발행되는 것을 확인했습니다.

```text
ORDER_PLACED       → tradecore.order.placed
ORDER_CANCELLED    → tradecore.order.cancelled
EXECUTION_APPLIED  → tradecore.execution.applied
```

---

## 5. 동시성 제어 전략

TradeCore는 도메인 모델부터 DB 계층까지 일관된 **Optimistic Locking** 전략을 적용합니다.

### 코어 레벨
- `Account`, `Order`는 각각 `version` 필드를 가짐
- Fake Repository에서 version 불일치 시 `ConcurrencyConflictException` 발생

### DB 레벨 (`tradecore-db`)
- `OrderEntity`, `AccountEntity`에 JPA `@Version` 적용
- `AccountEntity`는 `OPTIMISTIC_FORCE_INCREMENT` + `flush` + `refresh` 조합으로 잔고 변경 시 version 강제 증분
- `OrderEntity`는 `saveAndFlush()` 호출 시 JPA `@Version`이 자동 증분
- 동시 수정 충돌 시 `ObjectOptimisticLockingFailureException` → `ConcurrencyConflictException` 변환
- Testcontainers 환경에서 실제 PostgreSQL로 충돌 시나리오 검증

```java
// OrderEntity.java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

---

## 6. 멱등성 처리 전략

체결 반영은 외부 시스템 재시도, 메시지 중복 전달, 네트워크 재전송이 자주 발생하는 영역입니다.
`ApplyExecutionService`는 `executionId`를 기준으로 다음 규칙을 적용합니다.

```
같은 executionId + 같은 내용  → 기존 결과 반환 (멱등)
같은 executionId + 다른 내용  → ConcurrencyConflictException (충돌)
```

이 전략은 향후 Kafka consumer, outbox relay, 재처리 배치에서도 그대로 활용 가능한 구조입니다.

---

## 7. REST API

### 공통 에러 응답 형식 (RFC 7807 Problem Details)

모든 에러는 `ProblemDetail` 형식으로 반환됩니다.

```json
{
  "type": "https://example.com/problems/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "주문을 찾을 수 없습니다. orderId=...",
  "timestamp": "2026-04-07T10:00:00Z",
  "path": "/api/orders/..."
}
```

| HTTP 상태 | 원인 |
|---|---|
| 400 Bad Request | 유효성 검증 실패, 잔고 부족, 비정상 상태 전이 |
| 404 Not Found | 존재하지 않는 리소스 |
| 409 Conflict | 동시성 충돌, 이미 종결된 주문 |
| 500 Internal Server Error | 서버 내부 오류 |

---

### 계정 조회

#### `GET /api/accounts/{accountId}`

**응답 예시**
```json
{
  "accountId": "demo-user-1",
  "version": 3,
  "balances": {
    "BTC":  { "available": "1.3", "locked": "0.2", "total": "1.5" },
    "ETH":  { "available": "10",  "locked": "0",   "total": "10"  },
    "USDT": { "available": "95000", "locked": "5000", "total": "100000" }
  }
}
```

---

### 주문 목록 조회

#### `GET /api/accounts/{accountId}/orders`

계정 기준으로 주문 목록을 조회합니다. 필터 조건은 모두 선택 사항이며, 결과는 `createdAt` 내림차순으로 정렬됩니다.

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 | 기본값 |
|---|---|---|---|
| `symbol` | N | 심볼 필터 (예: `BTCUSDT`) | — |
| `status` | N | 주문 상태 필터 (`NEW`, `PARTIALLY_FILLED`, `FILLED`, `CANCELLED`) | — |
| `side` | N | 주문 방향 필터 (`BUY`, `SELL`) | — |
| `page` | N | 페이지 번호 (0부터 시작) | `0` |
| `size` | N | 페이지 크기 (1 ~ 100) | `20` |

**요청 예시**
```
GET /api/accounts/demo-user-1/orders?symbol=BTCUSDT&status=NEW&page=0&size=10
```

**응답 예시** (HTTP 200 OK)
```json
{
  "content": [
    {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "accountId": "demo-user-1",
      "symbol": "BTCUSDT",
      "side": "BUY",
      "orderType": "LIMIT",
      "status": "NEW",
      "price": "90000",
      "qty": "0.1",
      "filledQty": "0",
      "remainingQty": "0.1",
      "version": 1,
      "createdAt": "2026-04-13T10:00:00Z",
      "updatedAt": "2026-04-13T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

---

### 주문

#### `POST /api/orders` — 주문 생성 (현재 LIMIT 주문만 지원)

**요청 바디**
```json
{
  "accountId":  "demo-user-1",
  "symbol":     "BTCUSDT",
  "side":       "BUY",
  "orderType":  "LIMIT",
  "price":      "90000",
  "qty":        "0.1"
}
```

**응답 예시** (HTTP 201 Created)
```json
{
  "orderId":    "550e8400-e29b-41d4-a716-446655440000",
  "accountId":  "demo-user-1",
  "symbol":     "BTCUSDT",
  "side":       "BUY",
  "orderType":  "LIMIT",
  "status":     "NEW",
  "price":      "90000",
  "qty":        "0.1",
  "filledQty":  "0",
  "remainingQty": "0.1",
  "version":    1
}
```

#### `POST /api/orders/{orderId}/cancel` — 주문 취소

**요청 바디**
```json
{
  "accountId": "demo-user-1"
}
```

**응답 예시** (HTTP 200 OK)
```json
{
  "orderId":    "550e8400-...",
  "status":     "CANCELLED",
  "filledQty":  "0.05",
  "remainingQty": "0",
  "version":    3
}
```

#### `GET /api/orders/{orderId}` — 단건 주문 조회

---

### 체결 반영

#### `POST /api/executions` — 체결 반영 (현재 LIMIT 주문만 지원)

**요청 바디**
```json
{
  "executionId":   "exec-001",
  "orderId":       "550e8400-...",
  "executionPrice": "89500",
  "executionQty":  "0.05"
}
```

**응답 예시** (HTTP 200 OK)
```json
{
  "executionId":     "exec-001",
  "orderId":         "550e8400-...",
  "accountId":       "demo-user-1",
  "symbol":          "BTCUSDT",
  "side":            "BUY",
  "executionPrice":  "89500",
  "executionQty":    "0.05",
  "quoteAmount":     "4475",
  "executedAt":      "2026-04-07T10:15:30Z",
  "orderStatus":     "PARTIALLY_FILLED",
  "orderFilledQty":  "0.05",
  "orderRemainingQty": "0.05",
  "balances": {
    "BTC":  { "available": "1.55", "locked": "0" },
    "USDT": { "available": "95025", "locked": "4500" }
  }
}
```

---

## 8. DB 스키마 (Flyway)

Flyway가 활성화되어 있으며 애플리케이션 시작 시 자동으로 마이그레이션을 실행합니다.

| 버전 | 파일 | 내용 |
|---|---|---|
| V1 | `V1__init_tradecore_schema.sql` | `accounts`, `account_balances`, `orders`, `executions` 테이블 생성, 인덱스 생성 |
| V2 | `V2__add_order_audit_columns.sql` | `orders` 테이블에 `created_at`, `updated_at` 컬럼 추가, 복합 인덱스 `(account_id, created_at desc)` 생성 |
| V3 | `V3__create_outbox_event.sql` | `outbox_event` 테이블 생성, 상태/생성일 기준 조회 인덱스와 aggregate 추적 인덱스 생성 |

```sql
-- orders 테이블 주요 구조
create table if not exists orders (
    order_id    varchar(100)    not null,
    account_id  varchar(100)    not null,
    base_asset  varchar(20)     not null,
    quote_asset varchar(20)     not null,
    side        varchar(20)     not null,
    order_type  varchar(20)     not null,
    status      varchar(30)     not null,
    price       numeric(30, 12),
    qty         numeric(30, 12) not null,
    filled_qty  numeric(30, 12) not null,
    version     bigint          not null,
    created_at  timestamptz     not null default now(),
    updated_at  timestamptz     not null default now(),
    constraint pk_orders primary key (order_id)
);
```

```sql
-- outbox_event 테이블 주요 구조
create table outbox_event
(
    event_id        varchar(64) primary key,
    aggregate_type  varchar(50)  not null,
    aggregate_id    varchar(64)  not null,
    event_type      varchar(100) not null,
    payload         jsonb        not null,
    status          varchar(20)  not null,
    attempt_count   integer      not null default 0,
    last_error      text,
    created_at      timestamptz  not null,
    published_at    timestamptz
);
```

---

## 9. 데모 계정

애플리케이션 최초 실행 시 `DemoDataInitializer`가 아래 계정을 자동 생성합니다.

| accountId | 자산 | 초기 잔고 |
|---|---|---|
| `demo-user-1` | USDT | 100,000 |
| `demo-user-1` | BTC  | 1.5 |
| `demo-user-1` | ETH  | 10 |

---

## 10. 테스트 구조

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
| `ExecutionRepositoryAdapterTest` | 체결 기록 저장/조회, 중복 ID 충돌 감지 |
| `OrderQueryRepositoryAdapterTest` | 계정별 주문 조회, 관리자 전체 주문 검색, 동적 필터 검색, 페이지네이션, 정렬 검증 |
| `OutboxEventRepositoryAdapterTest` | Outbox 이벤트 저장, pending 조회, published/failed 상태 변경 검증 |

### API Test (`tradecore-api`, MockMvc)

MockMvc 기반으로 컨트롤러 레이어를 격리 검증합니다.

| 테스트 클래스 | 검증 내용 |
|---|---|
| `AccountQueryControllerTest` | 계정 조회, 주문 목록 조회 (필터/페이지네이션), 404 처리 |
| `OrderCommandControllerTest` | 주문 생성, 취소, 유효성 검증, 에러 응답 |
| `OrderQueryControllerTest` | 단건 주문 조회, 404 처리 |
| `ExecutionCommandControllerTest` | 체결 반영, 멱등 처리, 에러 응답 |

포트폴리오 관점에서 이 프로젝트는 "기능이 된다"보다 **정합성이 깨지지 않는다**를 테스트로 증명하는 방향을 강조합니다.

### Infra Test (`tradecore-infra`)

Kafka를 실제로 띄우지 않아도 Outbox Relay의 핵심 동작과 조건부 Bean 등록을 검증합니다.

| 테스트 클래스 | 검증 내용 |
|---|---|
| `OutboxRelayTest` | PENDING 이벤트 발행, PUBLISHED 처리, 발행 실패 시 markFailed 처리, batchSize 제한 |
| `OutboxTopicResolverTest` | eventType별 Kafka topic 매핑, 미지원 eventType 예외 처리 |
| `OutboxRelayConditionTest` | `tradecore.outbox.relay.enabled` 설정에 따른 Relay Bean 등록 조건 검증 |
| `KafkaOutboxEventPublisherConditionTest` | KafkaTemplate 존재 여부와 enabled 설정에 따른 Kafka Publisher Bean 등록 조건 검증 |

---

## 11. 실행 방법

### 사전 요구사항

- Java 25+
- Docker (DB 통합 테스트용)
- PostgreSQL (API 실행용, 또는 Docker Compose 사용)

### DB 설정

`tradecore-api/src/main/resources/application.yml`에서 데이터소스를 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cmp?currentSchema=tradecore
    username: cmp
    password: cmp
    driver-class-name: org.postgresql.Driver

  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: tradecore
    schemas: tradecore

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: tradecore
        format_sql: true
        jdbc:
          time_zone: UTC
    show-sql: true

  sql:
    init:
      mode: never

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

logging:
  level:
    org.hibernate.SQL: debug

tradecore:
  outbox:
    relay:
      enabled: true
      batch-size: 20
      fixed-delay-ms: 5000
    topics:
      order-placed: tradecore.order.placed
      order-cancelled: tradecore.order.cancelled
      execution-applied: tradecore.execution.applied
```

> Flyway가 활성화되어 있어 `ddl-auto: validate` 모드로 동작합니다. 스키마를 수동으로 생성할 필요 없이 애플리케이션 시작 시 자동 마이그레이션됩니다.

### API 실행

```bash
./gradlew :tradecore-api:bootRun
```

### 헬스 체크

```bash
curl http://localhost:8080/api/ping
# → pong
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

## 12. 설계 문서

### `docs/concurrency-policy.md`

코드 작성에 앞서 동시성 정책을 문서로 먼저 정의했습니다.

- 주문 취소와 체결의 충돌 정책
- 계정 잔고 lock/unlock 정합성 원칙
- executionId 기반 멱등성 규칙
- 충돌 발생 시 예외 처리 원칙
- version 전략과 DB 반영 방향

---

## 13. 이 프로젝트에서 강조하고 싶은 점

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

### 6) Command / Query 분리
명령(주문 생성/취소/체결)과 조회(계정/주문 목록)를 `TradingCommandFacade` / `TradingQueryService`로 분리하고, 조회 전용 포트(`OrderQueryRepository`)와 동적 JPQL 어댑터(`OrderQueryRepositoryAdapter`)를 별도로 운영합니다.

---

## 14. 향후 확장 계획

### 단기

- Outbox Relay 안정화
    - `FAILED` / `PENDING` 이벤트 재시도 정책 정리
    - `attempt_count` 제한 및 최종 실패 처리 정책 추가
    - `last_error` 기록 형식 정리
    - Relay 운영 로그와 Micrometer 메트릭 추가
- 다중 인스턴스 대비 Outbox 조회 개선
    - `FOR UPDATE SKIP LOCKED` 기반 중복 발행 방지
    - Relay batch 처리 트랜잭션 경계 정리
- Kafka Testcontainers 기반 통합 테스트 추가
- Swagger / OpenAPI 문서 자동화

### 중기

- Redis 기반 분산락 / idempotency 지원
- Kafka consumer 기반 execution 처리 흐름 확장
- Outbox 이벤트 재처리/복구 API 또는 운영 배치 설계
- 관리자용 이벤트 발행 이력 조회 API 추가

### 장기

- 매칭 엔진 또는 외부 execution source와 연동
- 리스크 체크 / 주문 제한 정책 확장
- 성능 테스트 및 장애 시나리오 검증
- 관측 가능성 강화: Prometheus, Grafana, structured logging

---

## 15. 한 줄 요약

**TradeCore는 "주문이 들어오면 저장한다" 수준이 아니라, 거래 시스템에서 가장 중요한 정합성과 동시성 문제를 Java 도메인 모델 → JPA 영속성까지 일관되게 설계하고 테스트로 증명한 Trading Core 프로젝트입니다.**