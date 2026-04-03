package com.jhpark.tradecore.db.repository;

import com.jhpark.tradecore.db.entity.account.AccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    @EntityGraph(attributePaths = "balances")
    Optional<AccountEntity> findWithBalancesByAccountId(String accountId);
}
