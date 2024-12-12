package com.pnevsky.msidentity.controller;

import com.pnevsky.msidentity.dto.AuthRequest;
import com.pnevsky.msidentity.dto.RegistrationRequest;
import com.pnevsky.msidentity.entity.UserCredential;
import com.pnevsky.msidentity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegistrationRequest request) {
        var credential = new UserCredential();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(request.getPassword());
        authService.saveUser(credential);
    }

    @PostMapping("/token")
    public TokenResponse getToken(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()));
        return new TokenResponse("Bearer", authService.generateToken(request.getUsername()));
    }

    @GetMapping("/validate")
    public String validateToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }
        authService.validateToken(authorization.substring(7));
        return "Token is valid";
    }

    public record TokenResponse(String tokenType, String accessToken) {}
}
