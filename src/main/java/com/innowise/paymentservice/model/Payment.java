package com.innowise.paymentservice.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    private String id;

    @Indexed
    @Field("order_id")
    private Long orderId;

    @Indexed
    @Field("user_id")
    private Long userId;

    @Indexed
    private PaymentStatus status;

    @Indexed
    private LocalDateTime timestamp;

    @Field("payment_amount")
    private Double paymentAmount;
}

