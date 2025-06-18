package com.collegeproject.chatgptclone.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set; // Used for roles

@Data // Lombok to generate getters, setters, etc.
@Document(collection = "users") // Maps this class to a MongoDB collection named "users"
public class User {

    @Id // Marks this field as the primary identifier
    private String id;
    private String username; // User's unique username
    private String password; // Hashed password
    private Set<String> roles; // Roles (e.g., "USER", "ADMIN"). Using Set<String> for simplicity.

    // No-arg constructor required by Spring Data
    public User() {
    }

    // Constructor for creating a new user
    public User(String username, String password, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }
}
