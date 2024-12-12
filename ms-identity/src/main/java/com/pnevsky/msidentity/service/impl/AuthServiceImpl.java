package com.pnevsky.msidentity.service.impl;

import com.pnevsky.msidentity.entity.UserCredential;
import com.pnevsky.msidentity.repository.UserCredentialRepository;
import com.pnevsky.msidentity.service.AuthService;
import com.pnevsky.msidentity.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public String saveUser(UserCredential credential) {
        if (credential.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds BCrypt byte limit");
        }
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        return "User registered";
    }

    @Override
    public String generateToken(String username) { return jwtService.generateToken(username); }

    @Override
    public void validateToken(String token) { jwtService.validateToken(token); }
}
