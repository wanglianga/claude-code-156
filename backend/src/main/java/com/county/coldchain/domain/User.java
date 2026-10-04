package com.county.coldchain.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sys_user")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String passwordHash;
    private String name;

    /** ADMIN / QC / WAREHOUSE / FINANCE */
    private String role;

    public User(String username, String passwordHash, String name, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }
}
