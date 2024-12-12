package com.pnevsky.msidentity.controller;

import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidFields() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid registration or authentication fields");
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ProblemDetail unauthorized(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials or token");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail duplicateUser() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username or email is already registered");
    }
}
