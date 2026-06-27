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

        // Unified response to prevent email enumeration
        String genericMessage = "If your email is eligible, you'll hear from us shortly.";

        if (userRepository.findByEmail(email).isPresent()) {
            // Already a user – no indication given, just generic.
            return ResponseEntity.ok(Map.of("message", genericMessage));
        }

        if (accessRequestRepository.existsByEmail(email)) {
            // Already requested – still same message.
            return ResponseEntity.ok(Map.of("message", genericMessage));
        }

        try {
            AccessRequest request = new AccessRequest();
            request.setEmail(email);
            accessRequestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            // Concurrent request – fine, ignore.
            return ResponseEntity.ok(Map.of("message", genericMessage));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", genericMessage));
    }
}