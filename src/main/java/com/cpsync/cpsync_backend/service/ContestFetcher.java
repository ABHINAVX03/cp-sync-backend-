package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;

import java.util.List;

public interface ContestFetcher {
    Platform getPlatform();
    List<ContestDto> fetchUpcomingContests();
}