package com.zoo.care.exception;

public class EmailAlreadyRegisteredException extends BusinessRuleException {
    public EmailAlreadyRegisteredException() {
        super("Email already registered");
    }
}