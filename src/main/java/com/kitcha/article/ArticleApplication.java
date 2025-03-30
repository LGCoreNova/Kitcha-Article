package com.kitcha.article;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ArticleApplication {
    
    private static final Logger log = LoggerFactory.getLogger(ArticleApplication.class);
    
    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(ArticleApplication.class, args);
    }
    
    @PostConstruct
    public void init() {
        log.info("RabbitMQ 연결 설정: host={}, port={}", 
            environment.getProperty("spring.rabbitmq.host"),
            environment.getProperty("spring.rabbitmq.port"));
        log.info("Cloud Bus 설정: enabled={}", 
            environment.getProperty("spring.cloud.bus.enabled"));
    }
}
