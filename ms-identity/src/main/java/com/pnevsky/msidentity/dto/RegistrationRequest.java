package com.pnevsky.msidentity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank @Size(max = 64)
    private String username;
    @NotBlank @Email @Size(max = 255)
    private String email;
    @NotBlank @Size(min = 8, max = 64)
    private String password;
}
