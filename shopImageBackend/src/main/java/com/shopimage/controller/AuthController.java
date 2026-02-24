package com.shopimage.controller;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.dto.AuthResponse;
import com.shopimage.dto.LoginRequest;
import com.shopimage.dto.RegisterRequest;
import com.shopimage.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Registration request for username: {}", request.getUsername());
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login request for username: {}", request.getUsername());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<String[]>> getRoles() {
        String[] roles = {"ADMIN", "USER", "MANAGER"};
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }
}