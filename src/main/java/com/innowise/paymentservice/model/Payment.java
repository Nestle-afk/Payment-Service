package com.innowise.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id"),
                @Index(name = "idx_payments_user_id", columnList = "user_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_timestamp", columnList = "timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "payment_amount")
    private Double paymentAmount;
}
