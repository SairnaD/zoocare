package com.zoo.care.exception;

public class AnimalAlreadyExistsException extends BusinessRuleException {
    public AnimalAlreadyExistsException(String name) {
        super("Animal with name '" + name + "' already exists");
    }
}