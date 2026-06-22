package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.service.ContestAggregatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestAggregatorService aggregatorService;

    public ContestController(ContestAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @GetMapping
    public List<ContestDto> getAllUpcomingContests() {
        return aggregatorService.fetchAllContests();
    }
}