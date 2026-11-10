package com.zoo.care.repository;

import java.time.Instant;

public interface LastFeedingTime {
    Long getAnimalId();
    Instant getLastFedAt();
}
