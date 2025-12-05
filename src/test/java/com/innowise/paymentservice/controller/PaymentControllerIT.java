package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.dto.PaymentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "external.random-api.url=http://localhost:8089/random"
})
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    static WireMockServer wireMockServer = new WireMockServer(0);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);

        registry.add("random.api.url", () -> "http://localhost:8089/random");
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

//    @BeforeEach
//    void stubRandomApi() {
//        wireMockServer.resetAll();
//
//        stubFor(WireMock.get(urlEqualTo("/random"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withBody("[42]")));
//    }

    @Test
    @Order(1)
    void createPayment_successful() throws Exception {

        stubFor(WireMock.get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[42]")));

        PaymentRequest request = new PaymentRequest(
                1001L,
                555L,
                300.0
        );

        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content(mapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.paymentAmount").value(300.0))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @Order(2)
    void getPaymentsByStatuses() throws Exception {

        mockMvc.perform(
                        get("/payments")
                                .param("statuses", "SUCCESS")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @Order(3)
    void getPaymentsByOrderId() throws Exception {

        mockMvc.perform(
                        get("/payments/orders/{orderId}", 1001)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1001))
                .andExpect(jsonPath("$[0].paymentAmount").value(300.0))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @Order(4)
    void getPaymentsByUserId() throws Exception {

        mockMvc.perform(
                        get("/payments/users/{userId}", 555)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1001))
                .andExpect(jsonPath("$[0].paymentAmount").value(300.0))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @Order(5)
    void getStatistics() throws Exception {

        String start = LocalDateTime.now().minusDays(1).toString();
        String end = LocalDateTime.now().plusDays(1).toString();

        mockMvc.perform(
                        get("/payments/statistics")
                                .param("start", start)
                                .param("end", end)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("300.0"));
    }
}
