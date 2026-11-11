package com.zoo.care.exception;

public class FeedingTimeInFutureException extends BusinessRuleException {
    public FeedingTimeInFutureException() {
        super("Feeding time cannot be in the future");
    }
}