# TradeCore

**거래 정합성과 동시성 제어에 집중한 Java 25 기반 Trading Core 프로젝트**

TradeCore는 암호화폐/증권형 주문 시스템에서 가장 까다로운 영역인 **주문 생성, 자산 lock/unlock, 체결 반영, 상태 전이, 멱등성, 동시성 충돌 처리**를 별도의 코어 프로젝트로 분리해 설계한 포트폴리오 프로젝트입니다.  
기존의 실시간 시세/차트 프로젝트인 **Crypto Market Pipeline**에서 시장 데이터 흐름을 담당했다면, TradeCore는 그 위에서 실제 거래의 핵심이 되는 **Order / Account / Balance / Execution** 정합성을 담당하는 역할로 분리되었습니다.

---

## 1. 프로젝트 개요

실제 거래 시스템에서는 단순히 주문을 저장하는 것보다 아래 문제가 더 중요합니다.

- 동시에 들어온 주문/취소/체결 요청이 충돌할 때 어떻게 정합성을 지킬 것인가
- 동일 체결 요청이 재전송되었을 때 어떻게 멱등하게 처리할 것인가
- BUY/SELL에 따라 어떤 자산을 lock/unlock 해야 하는가
- 체결 시 주문 상태와 잔고 상태를 어떻게 함께 안전하게 변경할 것인가

TradeCore는 이 문제를 **도메인 모델 중심 구조 + 애플리케이션 서비스 + 버전 기반 충돌 감지**로 풀어가는 프로젝트입니다.

---

## 2. 프로젝트 목표

### 핵심 목표
- 거래 코어를 시장 데이터 처리 계층과 분리
- 주문/잔고/체결의 상태 정합성 보장
- 동시성 충돌을 명시적으로 다루는 구조 설계
- idempotency, optimistic locking, state transition을 코드 레벨에서 검증
- 이후 PostgreSQL + Redis + Kafka 기반으로 확장 가능한 구조 확보

### 포트폴리오 포인트
- 단순 CRUD가 아닌 **거래 도메인 문제 해결 능력** 강조
- **동시성 / 멱등성 / 상태 전이 / 정합성** 중심 설계 경험 표현
- Java 기반 도메인 모델링과 테스트 설계 역량 표현
- 향후 실거래/가상거래/주문 매칭 시스템으로 확장 가능한 기반 제시

---

## 3. 기술 스택

### 현재 적용
- **Java 25**
- **Gradle 9.4**
- **Spring Boot 4.0.4**
- **JUnit 5**
- 멀티모듈 구조

### 현재 구조에 반영된 방향성
- **Core**: 순수 도메인 + 애플리케이션 서비스
- **API**: Spring Boot 진입점 및 HTTP 레이어 확장 예정
- **Infra**: Redis / Kafka 등 외부 연동 예정
- **DB**: 영속성 모듈 분리 예정

### 향후 확장 예정
- **PostgreSQL**: 주문/계정/체결 영속화
- **Redis**: 분산락, 캐시, idempotency 보조
- **Kafka**: outbox/event 기반 비동기 확장
- **Observability**: Micrometer, Prometheus, Grafana

> 현재 시점의 구현은 거래 코어와 테스트에 집중되어 있으며, DB/Infra는 모듈만 분리된 초기 상태입니다.

---

## 4. 프로젝트 구조

```text
tradecore
├─ tradecore-api      # Spring Boot 앱 진입점, API 레이어 확장 예정
├─ tradecore-core     # 핵심 도메인 및 유스케이스
├─ tradecore-db       # DB 모듈 자리 (초기 스켈레톤)
├─ tradecore-infra    # Redis/Kafka 등 인프라 모듈 자리
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
   ├─ order     # Place / Cancel / ApplyExecution 서비스
   ├─ port.out  # Repository 포트
   └─ exception # ConcurrencyConflictException
```

---

## 5. 현재 구현 범위

### 5-1. Account / Balance 도메인
TradeCore는 계정의 자산 상태를 `available` / `locked`로 분리하여 관리합니다.

- `Balance.lock(amount)`
- `Balance.unlock(amount)`
- `Balance.decreaseLocked(amount)`
- `Balance.increaseAvailable(amount)`
- `Account.lock(asset, amount)`
- `Account.unlock(asset, amount)`

이를 통해 주문 생성 시 자산을 잠그고, 취소 또는 체결 시 잠금 자산을 해제/정산하는 흐름을 명확하게 표현했습니다.

### 5-2. Order 도메인
주문은 다음 상태 전이를 갖습니다.

- `NEW`
- `PARTIALLY_FILLED`
- `FILLED`
- `CANCELLED`

구현 포인트:
- 지정가/시장가 주문 생성 팩토리 분리
- `cancel()`로 허용된 상태에서만 취소 가능
- `applyFill()`로 누적 체결 수량 반영
- `remainingQty()` 계산 지원
- 종결 주문(FILLED, CANCELLED)에 대한 추가 변경 차단

### 5-3. 주문 생성 유스케이스
`PlaceOrderService`

현재는 **LIMIT 주문만 지원**합니다.

- BUY 주문: quote asset 잠금
- SELL 주문: base asset 잠금
- 주문 저장 전 계정 잔고를 먼저 lock
- lock 이후 Order 생성 및 저장

즉, 주문 생성 단계에서 이미 **실행 가능한 자산이 확보된 주문**만 시스템에 들어오도록 설계했습니다.

### 5-4. 주문 취소 유스케이스
`CancelOrderService`

- 주문 소유 계정 검증
- 미체결 잔량 기준 unlock amount 계산
- BUY는 quote asset, SELL은 base asset 반환
- 주문 상태를 `CANCELLED`로 전이

이를 통해 부분 체결 이후 남은 수량만큼만 자산이 해제되도록 설계했습니다.

### 5-5. 체결 반영 유스케이스
`ApplyExecutionService`

현재는 **LIMIT 주문 체결 반영**까지 구현되어 있습니다.

핵심 처리:
- `executionId` 기준 중복 요청 확인
- 동일 executionId + 동일 내용이면 멱등 처리
- 동일 executionId + 다른 내용이면 충돌 예외 발생
- 주문 상태 검증 및 미체결 수량 검증
- BUY 체결 시:
  - 잠겨 있던 quote asset 차감
  - base asset 증가
  - 주문가 대비 더 낮은 가격에 체결된 경우 refund 처리
- SELL 체결 시:
  - 잠겨 있던 base asset 차감
  - quote asset 증가
- 주문의 `filledQty`, `status` 동시 반영

즉, 체결 반영은 단순 상태 변경이 아니라 **주문 상태 + 계정 잔고 + execution 기록**을 함께 갱신하는 핵심 유스케이스로 설계되어 있습니다.

---

## 6. 동시성 제어 전략

TradeCore는 현재 코드 레벨에서 **version 기반 optimistic locking 모델**을 반영하고 있습니다.

### 핵심 아이디어
- `Account`, `Order`는 각각 `version` 필드를 가짐
- 저장 시 현재 버전과 요청 버전이 다르면 충돌로 간주
- 충돌은 `ConcurrencyConflictException`으로 명시적으로 표현

### 테스트용 Fake Repository에서 검증하는 내용
- 동일 snapshot으로 두 번 저장 시 두 번째 저장 실패
- 저장 직전에 버전이 증가한 상황을 시뮬레이션하여 충돌 발생 검증
- Account / Order 모두 버전 충돌 전파 검증

이 구조는 이후 실제 DB 계층에서 `@Version` 또는 conditional update로 자연스럽게 확장될 수 있도록 의도했습니다.

---

## 7. 멱등성 처리 전략

체결 반영은 외부 시스템 재시도, 메시지 중복 전달, 네트워크 재전송이 자주 발생하는 영역입니다.  
이를 고려해 `ApplyExecutionService`는 `executionId`를 기준으로 다음 규칙을 적용합니다.

- 같은 `executionId`로 같은 내용이 다시 들어오면 기존 결과를 반환
- 같은 `executionId`로 다른 `orderId`, `price`, `qty`가 들어오면 충돌 처리

이 전략은 향후 Kafka consumer, outbox relay, 재처리 배치에서도 그대로 활용 가능한 구조입니다.

---

## 8. 테스트 포인트

현재 프로젝트는 거래 코어 설계 검증을 위해 테스트를 함께 작성하고 있습니다.

### 테스트 대상
- `BalanceTest`
- `AccountTest`
- `SymbolTest`
- `OrderTest`
- `PlaceOrderServiceTest`
- `CancelOrderServiceTest`
- `ApplyExecutionServiceTest`
- `FakeAccountRepositoryTest`
- `FakeOrderRepositoryTest`

### 검증하는 시나리오 예시
- available/locked 잔고 계산
- 주문 상태 전이 검증
- BUY/SELL 주문 시 lock 자산 계산 검증
- 부분 체결 후 남은 수량 기준 취소 검증
- execution 중복 처리 및 충돌 검증
- account / order 저장 충돌 전파 검증

포트폴리오 관점에서 이 프로젝트는 “기능이 된다”보다 **정합성이 깨지지 않는다**를 테스트로 증명하는 방향을 강조합니다.

---

## 9. 현재 API 상태

`tradecore-api` 모듈은 현재 Spring Boot 애플리케이션 진입점과 헬스 체크용 `PingController`만 존재합니다.

- `GET /api/ping` → `pong`

이는 아직 HTTP 스펙 확장 전 단계이며, 현재 중심은 **도메인/유스케이스 안정화**입니다.

향후 추가 예정 예시:
- 주문 생성 API
- 주문 취소 API
- 체결 반영 API
- 계정/잔고 조회 API
- idempotency key 기반 요청 처리 API

---

## 10. 설계 문서

### `docs/concurrency-policy.md`
현재 프로젝트에는 동시성 정책 문서가 별도로 존재합니다.

주요 내용:
- 주문 취소와 체결의 충돌 정책
- 계정 잔고 lock/unlock 정합성 원칙
- executionId 기반 멱등성 규칙
- 충돌 발생 시 예외 처리 원칙
- version 전략과 향후 DB 반영 방향

코드 이전에 정책을 문서화해 두었다는 점도 이 프로젝트의 중요한 포트폴리오 포인트입니다.

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

### 전체 테스트 실행
```bash
./gradlew test
```

> Gradle wrapper가 처음 실행될 때는 로컬 환경에서 Gradle 배포본 다운로드가 필요할 수 있습니다.

---

## 12. 이 프로젝트에서 강조하고 싶은 점

### 1) 거래 도메인에 맞는 모델링
TradeCore는 이커머스식 상품/재고가 아니라, 거래 시스템에 맞는 개념인 `Symbol`, `Account`, `Balance`, `Order`, `Execution` 중심으로 모델링했습니다.

### 2) 상태 전이를 도메인에 내장
주문 상태 변경을 서비스 레이어에서 흩어 처리하지 않고, `Order` 도메인 내부 규칙으로 제한했습니다.

### 3) 동시성 충돌을 예외적인 사고가 아닌 정상 설계 요소로 취급
“나중에 DB 붙이고 생각하자”가 아니라, 초기부터 `version`, `ConcurrencyConflictException`, fake repository 충돌 시뮬레이션을 반영했습니다.

### 4) 테스트 가능한 코어 우선 설계
실제 DB/인프라 구현 전에도 거래 핵심 규칙을 충분히 검증할 수 있도록 core 중심으로 먼저 설계했습니다.

---

## 13. 향후 확장 계획

### 단기
- PostgreSQL 영속성 구현
- JPA `@Version` 또는 conditional update 반영
- 주문/체결 API 추가
- account / order / execution 조회 모델 추가

### 중기
- Redis 기반 분산락 / idempotency 지원
- Outbox 패턴 도입
- Kafka 이벤트 발행
- 재처리/복구 전략 설계

### 장기
- 매칭 엔진 또는 execution consumer와 연동
- 리스크 체크 / 주문 제한 정책 확장
- 감사 로그 / 운영자 조회 API 추가
- 성능 테스트 및 장애 시나리오 검증

---

## 14. 한 줄 요약

**TradeCore는 “주문이 들어오면 저장한다” 수준이 아니라, 거래 시스템에서 가장 중요한 정합성과 동시성 문제를 Java 도메인 모델과 테스트로 풀어낸 Trading Core 프로젝트입니다.**
