package com.zoo.care.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record KeeperSwitchRequest(

        @NotNull
        @Positive
        Long newKeeperId
) {
}
