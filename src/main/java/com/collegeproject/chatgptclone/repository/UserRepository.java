package com.collegeproject.chatgptclone.repository;

import com.collegeproject.chatgptclone.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marks this interface as a Spring Data repository
public interface UserRepository extends MongoRepository<User, String> {

    // Custom method to find a user by their username
    // Spring Data automatically generates the query for this method name
    Optional<User> findByUsername(String username);

    // Check if a user with a given username exists
    boolean existsByUsername(String username);
}
