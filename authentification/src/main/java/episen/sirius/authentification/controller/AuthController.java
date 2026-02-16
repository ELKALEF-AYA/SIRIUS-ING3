package episen.sirius.authentification.controller;

import episen.sirius.authentification.dto.LoginRequest;
import episen.sirius.authentification.dto.LoginResponse;
import episen.sirius.authentification.repository.UserAuthRepository;
import episen.sirius.authentification.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserAuthRepository repo;
    private final JwtService jwt;

    public AuthController(UserAuthRepository repo, JwtService jwt) {
        this.repo = repo;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        if (req == null || req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().build();
        }

        return repo.findByEmailAndPassword(req.email(), req.password())
                .map(u -> {
                    String token = jwt.generateToken(u.id(), u.email(), u.role(), u.tenantId());

                    return ResponseEntity.ok(
                            new LoginResponse(
                                    token,
                                    u.role(),
                                    u.id(),
                                    u.tenantId(),
                                    u.email(),
                                    u.firstName(),
                                    u.lastName()
                            )
                    );
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}