package com.innowise.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    @NotNull
    @Positive
    private Long orderId;

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Double paymentAmount;
}
