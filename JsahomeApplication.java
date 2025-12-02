package com.jsahome;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application principale JSAHome
 * Point d'entrée pour l'application Spring Boot
 */
@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.jsahome.auth.repository")
public class JsahomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsahomeApplication.class, args);
    }
}