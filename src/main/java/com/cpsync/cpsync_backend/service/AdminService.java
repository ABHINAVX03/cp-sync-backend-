package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final AccessRequestRepository accessRequestRepository;
    private final EmailService emailService;

    public AdminService(AccessRequestRepository accessRequestRepository,
                        EmailService emailService) {
        this.accessRequestRepository = accessRequestRepository;
        this.emailService = emailService;
    }

    public List<AccessRequest> getAllAccessRequests() {
        return accessRequestRepository.findAll();
    }

    @Transactional
    public String approveById(Long id) {
        AccessRequest request = accessRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));

        if (request.getApprovedAt() != null) {
            return "Already approved";
        }

        request.setApprovedAt(LocalDateTime.now());
        accessRequestRepository.save(request);
        emailService.sendWelcomeEmail(request.getEmail());
        return "Approved, email sent to " + request.getEmail();
    }

    @Transactional
    public void approveByEmail(String email) {
        AccessRequest request = accessRequestRepository.findByEmail(email)
                .orElseGet(() -> {
                    AccessRequest r = new AccessRequest();
                    r.setEmail(email);
                    return r;
                });
        request.setApprovedAt(LocalDateTime.now());
        accessRequestRepository.save(request);
        emailService.sendWelcomeEmail(email);
    }
}