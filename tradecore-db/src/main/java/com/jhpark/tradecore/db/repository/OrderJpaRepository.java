package com.jhpark.tradecore.db.repository;

import com.jhpark.tradecore.db.entity.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

}
