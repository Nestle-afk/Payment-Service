package com.innowise.paymentservice.service;

import com.innowise.paymentservice.config.RandomApiProperties;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RestTemplate restTemplate;
    private final RandomApiProperties randomApiProperties;

    public PaymentResponse createPayment(PaymentRequest request) {

        int randomNumber = fetchRandomNumber();

        PaymentStatus status;
        if (randomNumber % 2 == 0){
            status = PaymentStatus.SUCCESS;
        } else {
            status = PaymentStatus.FAILED;
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .paymentAmount(request.getPaymentAmount())
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        return paymentMapper.toResponse(saved);
    }

    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public List<PaymentResponse> getPaymentsByStatuses(List<PaymentStatus> statuses) {
        return paymentRepository.findByStatusIn(statuses)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public Double getTotalAmount(LocalDateTime start, LocalDateTime end) {
        Double result = paymentRepository.getTotalPaymentsSum(start, end);
        return result != null ? result : 0.0;
    }

    private int fetchRandomNumber() {
        try {
            Integer[] result = restTemplate.getForObject(randomApiProperties.getUrl(), Integer[].class);

            if (result == null || result.length == 0) {
                log.warn("Random API returned empty result, using fallback");
                return 1;
            }

            return result[0];

        } catch (Exception e) {
            log.error("Failed to call Random API, fallback value used", e);
            return 1;
        }
    }
}
