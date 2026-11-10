package com.zoo.care.exception;

import java.time.Instant;

public class FeedingTooEarlyException extends BusinessRuleException {
    public FeedingTooEarlyException(Instant nextAllowedAt) {
        super("Animal can be fed again at " + nextAllowedAt);
    }
}