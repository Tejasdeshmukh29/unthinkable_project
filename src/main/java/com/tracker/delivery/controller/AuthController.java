package com.tracker.delivery.controller;

import com.tracker.delivery.config.JwtUtils;
import com.tracker.delivery.dto.LoginRequest;
import com.tracker.delivery.dto.LoginResponse;
import com.tracker.delivery.dto.RegisterRequest;
import com.tracker.delivery.entity.Role;
import com.tracker.delivery.entity.User;
import com.tracker.delivery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            Role role = Role.valueOf("ROLE_" + request.getRole().toUpperCase());
            User user = User.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .role(role)
                    .build();

            User registeredUser = userService.register(user);
            return ResponseEntity.ok(Map.of("message", "User registered successfully", "userId", registeredUser.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateToken(request.getUsername());

            User user = userService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            return ResponseEntity.ok(new LoginResponse(jwt, user.getUsername(), user.getRole().name()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return userService.findByUsername(principal.getName())
                .map(user -> ResponseEntity.ok((Object) user))
                .orElse(ResponseEntity.notFound().build());
    }
}
