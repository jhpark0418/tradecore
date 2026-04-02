# Concurrency Policy

## 1. 목적
TradeCore는 주문, 잔고, 체결 처리 과정에서 동시 요청이 들어오더라도
자산 정합성과 주문 상태 정합성이 깨지지 않도록 한다.

핵심 원칙은 다음과 같다.

- 동일 요청의 중복 처리를 방지한다.
- 동시에 들어온 상충 작업은 둘 다 성공시키지 않는다.
- 충돌 발생 시 재시도 가능한 실패로 처리한다.
- 정합성을 우선하고, 부분 성공을 허용하지 않는다.

## 2. Order 정책
- 동일 주문에 대해 취소와 체결이 동시에 들어오면 둘 다 성공할 수 없다.
- 주문 상태 전이는 허용된 경로에서만 가능하다.
- 이미 종료된 주문(FILLED, CANCELLED)에는 추가 상태 변경을 허용하지 않는다.

## 3. Account 정책
- 같은 계정의 잔고에 대해 동시에 lock/unlock/update가 발생해도
  available + locked 정합성이 깨지면 안 된다.
- available 보다 많은 자산을 lock 할 수 없다.
- 충돌 상황에서는 하나의 요청만 성공하고 나머지는 실패해야 한다.

## 4. Execution 정책
- executionId는 전역적으로 한 번만 처리한다.
- 동일 executionId로 동일 요청이 다시 들어오면 멱등하게 처리한다.
- 동일 executionId로 다른 내용이 들어오면 충돌로 간주하고 실패한다.

## 5. 실패 처리 원칙
- 동시성 충돌은 비즈니스 검증 실패와 구분한다.
- 동시성 충돌은 ConcurrencyConflictException으로 표현한다.
- 애플리케이션 계층에서는 이 예외를 상위로 전파한다.

## 6. 향후 구현 방향
- DB 계층에서는 optimistic locking 또는 conditional update를 적용한다.
- executionId에는 unique constraint를 적용한다.
- 주문/잔고/체결 반영은 하나의 트랜잭션 경계 안에서 처리한다.

## 7. Versioning Strategy

TradeCore는 주문(Order)과 계정(Account)의 동시 수정 충돌을 감지하기 위해
낙관적 락 기반의 version 전략을 적용한다.

### 기본 원칙
- Account와 Order는 저장 시 version을 함께 관리한다.
- 조회 시점의 version과 저장 시점의 version이 다르면 충돌로 간주한다.
- 충돌 시 ConcurrencyConflictException을 발생시킨다.

### 적용 후보
1. Core port에서 expectedVersion을 명시적으로 전달
2. DB 구현체에서만 JPA @Version으로 처리

### 현재 방향
우선 DB 계층에서 @Version 기반으로 적용하고,
필요 시 core port에도 expectedVersion 개념을 확장한다.