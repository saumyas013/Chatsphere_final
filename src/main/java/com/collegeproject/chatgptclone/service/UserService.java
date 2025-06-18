package com.collegeproject.chatgptclone.service;

import com.collegeproject.chatgptclone.model.User;
import com.collegeproject.chatgptclone.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository; // Make final for constructor injection
    private final PasswordEncoder passwordEncoder; // Make final for constructor injection

    // NEW: Constructor for dependency injection
    // Spring will automatically inject UserRepository and PasswordEncoder here
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Implements Spring Security's UserDetailsService method to load user details by username.
     * This is crucial for authentication.
     *
     * @param username The username of the user to load.
     * @return UserDetails object containing user information.
     * @throws UsernameNotFoundException if the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsername(username);
        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    /**
     * Registers a new user in the system.
     * Hashes the password before saving.
     *
     * @param username The username for the new user.
     * @param password The raw password for the new user.
     * @return The saved User object, or null if username already exists.
     */
    public User registerNewUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return null;
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, encodedPassword, Collections.singleton("USER"));
        return userRepository.save(newUser);
    }
}
