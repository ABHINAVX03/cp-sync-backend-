package com.cpsync.cpsync_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlatformsRequest {
    @NotEmpty(message = "At least one platform must be specified")
    private List<String> platforms;
}
