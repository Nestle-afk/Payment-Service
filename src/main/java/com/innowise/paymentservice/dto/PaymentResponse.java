package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private Long orderId;

    private PaymentStatus status;

    private Double paymentAmount;

    private LocalDateTime timestamp;
}
