package com.example.chat_application.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.chat_application.Repositories")
@EnableMongoAuditing
public class MongoConfig {

}
