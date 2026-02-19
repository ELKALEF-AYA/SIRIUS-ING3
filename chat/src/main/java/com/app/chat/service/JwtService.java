package com.app.chat.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret doit contenir au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extraire le userId depuis le JWT token (JJWT 0.12.6)
     */
    public Long extractUserId(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            System.err.println("Erreur JWT extractUserId: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extraire l'email depuis le token
     */
    public String extractEmail(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("email", String.class);
        } catch (Exception e) {
            System.err.println("Erreur JWT extractEmail: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extraire le rôle depuis le token
     */
    public String extractRole(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("role", String.class);
        } catch (Exception e) {
            System.err.println("Erreur JWT extractRole: " + e.getMessage());
            return null;
        }
    }

    /**
     * Valider un token JWT
     */
    public boolean isTokenValid(String token) {
        return extractUserId(token) != null;
    }
}