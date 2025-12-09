package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.PaymentServiceApplication;
import com.innowise.paymentservice.dto.CreatePaymentEvent;
import com.innowise.paymentservice.dto.PaymentRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PaymentServiceApplication.class)
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

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    static WireMockServer wireMockServer = new WireMockServer(0);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);

        registry.add("external.random-api.url", () -> "http://localhost:8089/random");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
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

    @Test
    @Order(6)
    void createPayment_shouldPublishCreatePaymentEventToKafka() throws Exception {

        stubFor(WireMock.get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[24]")));

        PaymentRequest request = new PaymentRequest(
                2002L,
                777L,
                500.0
        );

        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content(mapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(2002))
                .andExpect(jsonPath("$.paymentAmount").value(500.0))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        try (Consumer<String, CreatePaymentEvent> consumer = createTestConsumer()) {
            consumer.subscribe(Collections.singletonList("create-payment-topic"));

            CreatePaymentEvent targetEvent = null;
            int attempts = 0;

            while (targetEvent == null && attempts < 5) {
                ConsumerRecords<String, CreatePaymentEvent> records =
                        consumer.poll(Duration.ofSeconds(2));

                for (var rec : records) {
                    CreatePaymentEvent ev = rec.value();
                    if (ev != null && ev.getOrderId() != null && ev.getOrderId().equals(2002L)) {
                        targetEvent = ev;
                        break;
                    }
                }

                attempts++;
            }

            assertThat(targetEvent)
                    .as("CREATE_PAYMENT event for orderId=2002 must be present in topic create-payment-topic")
                    .isNotNull();

            assertThat(targetEvent.getUserId()).isEqualTo(777L);
            assertThat(targetEvent.getPaymentAmount()).isEqualTo(500.0);
            assertThat(targetEvent.getStatus().name()).isEqualTo("SUCCESS");
        }
    }



    private Consumer<String, CreatePaymentEvent> createTestConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service-kafka-it-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CreatePaymentEvent.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<String, CreatePaymentEvent>(props)
                .createConsumer();
    }
}
