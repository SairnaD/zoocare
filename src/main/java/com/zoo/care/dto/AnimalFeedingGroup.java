package com.zoo.care.dto;

import java.util.List;

public record AnimalFeedingGroup(
        FeedingHistoryResponse latest,
        List<FeedingHistoryResponse> earlier) {
}