package com.cpsync.cpsync_backend.service.impl;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.service.ContestFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AtCoderFetcher implements ContestFetcher {

    private static final String URL = "https://atcoder.jp/contests/?lang=en";
    private static final Logger log = LoggerFactory.getLogger(AtCoderFetcher.class);

    // AtCoder time format: "2026-06-21 19:00:00+0900"
    private static final DateTimeFormatter START_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXX");

    @Override
    public Platform getPlatform() {
        return Platform.ATCODER;
    }


    @Override
    @Cacheable(value = "atcoderContests", unless = "#result.isEmpty()")
    public List<ContestDto> fetchUpcomingContests() {
        try {
            Document doc = Jsoup.connect(URL)
                    .userAgent("Mozilla/5.0 (compatible; CP-Bot/1.0)")
                    .timeout(10_000)
                    .get();

            Element upcomingDiv = doc.getElementById("contest-table-upcoming");
            if (upcomingDiv == null) {
                return Collections.emptyList();
            }

            List<ContestDto> contests = new ArrayList<>();
            Elements rows = upcomingDiv.select("tr");

            // skip header row (index 0)
            for (int i = 1; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");
                if (cols.size() < 3) continue;

                Element timeTag = cols.get(0).selectFirst("time");
                if (timeTag == null) continue;
                String startStr = timeTag.text().trim();

                Element link = cols.get(1).selectFirst("a");
                if (link == null) continue;
                String name = link.text().trim();
                String contestUrl = "https://atcoder.jp" + link.attr("href");

                String durationStr = cols.get(2).text().trim(); // "04:00"

                Instant startTime;
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(startStr, START_FORMAT);
                    startTime = odt.toInstant();
                } catch (Exception e) {
                    log.error("[{}] Fetch failed: {}", getPlatform(), e.getMessage(), e);
                    continue; // skip unparsable row
                }

                long durationSeconds;
                try {
                    String[] parts = durationStr.split(":");
                    long hours = Long.parseLong(parts[0]);
                    long minutes = Long.parseLong(parts[1]);
                    durationSeconds = (hours * 3600) + (minutes * 60);
                } catch (Exception e) {
                    log.error("[{}] Fetch failed: {}", getPlatform(), e.getMessage(), e);
                    durationSeconds = 2 * 3600; // fallback: 2 hours
                }

                // contest ID from URL, e.g. "/contests/abc123" -> "abc123"
                String contestId = contestUrl.substring(contestUrl.lastIndexOf("/") + 1);

                contests.add(new ContestDto(
                        Platform.ATCODER.name(),
                        contestId,
                        name,
                        startTime,
                        durationSeconds,
                        contestUrl
                ));
            }

            return contests;

        } catch (Exception e) {
            log.error("[{}] Fetch failed: {}", getPlatform(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
