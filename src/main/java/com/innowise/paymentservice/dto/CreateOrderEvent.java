package com.innowise.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private Double totalAmount;
}



