package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByOrderId(Long order_id);

    List<Payment> findByUserId(Long user_id);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    @Aggregation(pipeline = {
            "{ $match: { timestamp: { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: null, total: { $sum: \"$payment_amount\" } } }"
    })
    Double getTotalPaymentsSum(LocalDateTime startDate, LocalDateTime endDate);
}


