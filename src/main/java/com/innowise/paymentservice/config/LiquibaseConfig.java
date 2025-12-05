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
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @PostConstruct
    public void runLiquibase() throws Exception {
        DatabaseConnection connection = DatabaseFactory.getInstance()
                .openConnection(
                        mongoUri,
                        null,
                        null,
                        null,
                        null
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