package com.innowise.paymentservice.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @PostConstruct
    public void runLiquibase() throws Exception {
        // создаём Liquibase DatabaseConnection на основе mongodb URI
        DatabaseConnection connection = DatabaseFactory.getInstance()
                .openConnection(
                        mongoUri,      // mongodb://localhost:27017/payment_db?authSource=admin
                        null,          // username
                        null,          // password
                        null,          // driver
                        null           // default catalog
                );

        Database database = new MongoLiquibaseDatabase();
        database.setConnection(connection);

        Liquibase liquibase = new Liquibase(
                changeLog,
                new ClassLoaderResourceAccessor(),
                database
        );

        liquibase.update(new Contexts(), new LabelExpression());
    }
}