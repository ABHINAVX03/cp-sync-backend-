package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.request.AccessRequestDto;
import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccessRequestController {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;

    public AccessRequestController(AccessRequestRepository accessRequestRepository,
                                   UserRepository userRepository) {
        this.accessRequestRepository = accessRequestRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/request-access")
    @Transactional
    public ResponseEntity<Map<String, String>> requestAccess(
            @Valid @RequestBody AccessRequestDto dto) {

        String email = dto.getEmail();

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.ok(
                    Map.of("message", "You're already an active user. Please sign in directly."));
        }

        if (accessRequestRepository.existsByEmail(email)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "You've already requested access. We'll activate your account soon."));
        }

        try {
            AccessRequest request = new AccessRequest();
            request.setEmail(email);
            accessRequestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "You've already requested access. We'll activate your account soon."));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Your request has been received. We'll activate your account within 12 hours."));
    }
}
