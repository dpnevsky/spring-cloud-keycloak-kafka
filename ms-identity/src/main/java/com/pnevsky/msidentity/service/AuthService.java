package com.pnevsky.msidentity.service;

import com.pnevsky.msidentity.entity.UserCredential;

public interface AuthService {
    String saveUser(UserCredential credential);
    String generateToken(String username);
    void validateToken(String token);
}
