package com.innowise.paymentservice.consumer;

import com.innowise.paymentservice.dto.CreateOrderEvent;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "create-order-topic", groupId = "payment-service-group", 
                   containerFactory = "orderEventKafkaListenerContainerFactory")
    public void handleCreateOrderEvent(CreateOrderEvent event) {
        log.info("Received CREATE_ORDER event for order ID: {}, user ID: {}, total amount: {}", 
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        try {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(event.getOrderId());
            paymentRequest.setUserId(event.getUserId());
            paymentRequest.setPaymentAmount(event.getTotalAmount());

            paymentService.createPayment(paymentRequest);
            log.info("Payment processed successfully for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process payment for order ID: {}", event.getOrderId(), e);
        }
    }
}

