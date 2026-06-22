package com.cpsync.cpsync_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestDto {
    private String platform;      // "CODEFORCES", "LEETCODE", etc.
    private String contestId;     // platform-specific unique ID, used to build contestKey
    private String name;
    private Instant startTime;
    private long durationSeconds;
    private String url;

    public String getContestKey() {
        return platform + "_" + contestId;
    }
}