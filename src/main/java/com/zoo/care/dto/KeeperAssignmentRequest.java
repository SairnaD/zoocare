package com.zoo.care.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record KeeperAssignmentRequest(

        @NotNull
        @Positive
        Long keeperId
) {

}
