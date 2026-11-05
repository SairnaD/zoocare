package com.zoo.care.dto;

import java.time.Instant;
import com.zoo.care.entity.KeeperAssignment;

public record KeeperAssignmentResponse(
        Long id,
        Long enclosureId,
        Long keeperId,
        String keeperName,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt
) {

    public static KeeperAssignmentResponse from(KeeperAssignment assignment) {
        return new KeeperAssignmentResponse(
                assignment.getId(),
                assignment.getEnclosure().getId(),
                assignment.getKeeper().getId(),
                assignment.getKeeper().getFullName(),
                assignment.getStartsAt(),
                assignment.getEndsAt(),
                assignment.getCreatedAt()
        );
    }
}