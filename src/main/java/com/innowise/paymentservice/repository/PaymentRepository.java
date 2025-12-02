package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.OrderStatus;
import com.innowise.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByOrderId(Long orderId);

    List<Payment> findAllByUserId(Long userId);
    List<Payment> findAllByStatusIn(List<OrderStatus> statuses);

    @Query("""
        SELECT COALESCE(SUM(p.paymentAmount), 0)
        FROM Payment p
        WHERE p.timestamp BETWEEN :start AND :end
    """)
    Double getTotalAmountForPeriod(LocalDateTime start, LocalDateTime end);
}

