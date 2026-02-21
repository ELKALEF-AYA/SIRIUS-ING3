package episen.sirius.authentification.dto;

public record LoginResponse(
        String accessToken,
        String role,
        Long userId,
        Long tenantId,
        String email,
        String firstName,
        String lastName
) {}