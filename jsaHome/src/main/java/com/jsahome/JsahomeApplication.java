package com.jsahome;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale JSAHome
 * Point d'entrée pour l'application Spring Boot
 */
@SpringBootApplication
@EnableScheduling
public class JsahomeApplication {
    public static void main(String[] args) {
        SpringApplication.run(JsahomeApplication.class, args);
    }
}
