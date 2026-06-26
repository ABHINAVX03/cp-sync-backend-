package com.cpsync.cpsync_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;
}
