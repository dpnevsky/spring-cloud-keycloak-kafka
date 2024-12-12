package com.pnevsky.msidentity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credentials")
@Getter
@Setter
@NoArgsConstructor
public class UserCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, unique = true, length = 64)
    private String username;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, length = 60)
    private String password;
}
