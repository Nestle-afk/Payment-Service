package com.innowise.paymentservice.service;

import com.innowise.paymentservice.config.RandomApiProperties;
import com.innowise.paymentservice.dto.PaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RandomApiProperties randomApiProperties;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(randomApiProperties.getUrl()).thenReturn("http://fake-url.com");
    }

    @Test
    void createPayment_evenRandomNumber_success() {
        PaymentRequest request = new PaymentRequest(10L, 20L, 100.0);

        when(restTemplate.getForObject(anyString(), eq(Integer[].class)))
                .thenReturn(new Integer[]{42});

        Payment saved = Payment.builder()
                .orderId(10L)
                .userId(20L)
                .paymentAmount(100.0)
                .status(PaymentStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponse response = new PaymentResponse(
                10L,
                PaymentStatus.SUCCESS,
                100.0,
                saved.getTimestamp()
        );

        when(paymentMapper.toResponse(saved)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(100.0, result.getPaymentAmount());
    }

    @Test
    void createPayment_oddRandomNumber_failed() {
        PaymentRequest request = new PaymentRequest(10L, 20L, 100.0);

        when(restTemplate.getForObject(anyString(), eq(Integer[].class)))
                .thenReturn(new Integer[]{77});

        Payment saved = Payment.builder()
                .orderId(10L)
                .userId(20L)
                .paymentAmount(100.0)
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponse response = new PaymentResponse(
                10L,
                PaymentStatus.FAILED,
                100.0,
                saved.getTimestamp()
        );

        when(paymentMapper.toResponse(saved)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void createPayment_apiError_fallback() {
        PaymentRequest request = new PaymentRequest(10L, 20L, 100.0);

        when(restTemplate.getForObject(anyString(), eq(Integer[].class)))
                .thenThrow(new RuntimeException("API down"));

        Payment saved = Payment.builder()
                .orderId(10L)
                .userId(20L)
                .paymentAmount(100.0)
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponse response = new PaymentResponse(
                10L,
                PaymentStatus.FAILED,
                100.0,
                saved.getTimestamp()
        );

        when(paymentMapper.toResponse(saved)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void getPaymentsByOrderId_ok() {
        Payment p = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(p));
        when(paymentMapper.toResponse(p)).thenReturn(response);

        List<PaymentResponse> result = paymentService.getPaymentsByOrderId(10L);

        assertEquals(1, result.size());
    }

    @Test
    void getPaymentsByUserId_ok() {
        Payment p = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByUserId(20L)).thenReturn(List.of(p));
        when(paymentMapper.toResponse(p)).thenReturn(response);

        List<PaymentResponse> result = paymentService.getPaymentsByUserId(20L);

        assertEquals(1, result.size());
    }

    @Test
    void getPaymentsByStatuses_ok() {
        Payment p = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByStatusIn(List.of(PaymentStatus.SUCCESS)))
                .thenReturn(List.of(p));

        when(paymentMapper.toResponse(p)).thenReturn(response);

        List<PaymentResponse> result =
                paymentService.getPaymentsByStatuses(List.of(PaymentStatus.SUCCESS));

        assertEquals(1, result.size());
    }

    @Test
    void getTotalAmount_ok() {
        when(paymentRepository.getTotalPaymentsSum(any(), any())).thenReturn(250.5);

        Double result = paymentService.getTotalAmount(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        assertEquals(250.5, result);
    }

    @Test
    void getTotalAmount_null_returnsZero() {
        when(paymentRepository.getTotalPaymentsSum(any(), any())).thenReturn(null);

        Double result = paymentService.getTotalAmount(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        assertEquals(0.0, result);
    }
}
