package com.zoo.care.exception;

public class UsernameAlreadyTakenException extends BusinessRuleException {
    public UsernameAlreadyTakenException() {
        super("Username already taken");
    }
}