package com.innowise.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MongoIndexesConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoIndexesConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexesConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        log.info("Ensuring MongoDB indexes for 'payments' collection");
        mongoTemplate.indexOps("payments")
                .ensureIndex(new Index().on("order_id", org.springframework.data.domain.Sort.Direction.ASC).named("idx_order_id"));
        mongoTemplate.indexOps("payments")
                .ensureIndex(new Index().on("user_id", org.springframework.data.domain.Sort.Direction.ASC).named("idx_user_id"));
        mongoTemplate.indexOps("payments")
                .ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC).named("idx_status"));
        mongoTemplate.indexOps("payments")
                .ensureIndex(new Index().on("timestamp", org.springframework.data.domain.Sort.Direction.ASC).named("idx_timestamp"));
        log.info("Done ensuring indexes for 'payments'");

        mongoTemplate.indexOps("payments")
                .getIndexInfo()
                .forEach(info -> log.info("Index: {}", info));
    }
}