package com.jsahome;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Application principale JSAHome
 * Point d'entrée pour l'application Spring Boot
 */
@SpringBootApplication
@EnableCaching
public class JsahomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsahomeApplication.class, args);
    }
}