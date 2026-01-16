package episen.sirius.authentification.dto;

public record LoginResponse(String accessToken, String role, Long userId, String email) {}
