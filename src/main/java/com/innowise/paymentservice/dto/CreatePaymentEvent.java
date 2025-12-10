package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private Double paymentAmount;
    private PaymentStatus status;
}

