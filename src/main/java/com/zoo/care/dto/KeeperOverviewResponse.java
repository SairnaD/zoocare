package com.zoo.care.dto;

import com.zoo.care.entity.User;

import java.util.List;

public record KeeperOverviewResponse(
        Long id,
        String fullName,
        String username,
        String email,
        List<EnclosureSummary> enclosures,
        long animalCount
) {

    public static KeeperOverviewResponse from(User keeper, List<EnclosureSummary> enclosures, long animalCount) {
        return new KeeperOverviewResponse(keeper.getId(), keeper.getFullName(), keeper.getUsername(),
                keeper.getEmail(), enclosures, animalCount);
    }
}