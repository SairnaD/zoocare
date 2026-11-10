package com.zoo.care.exception;

public class EnclosureAlreadyExistsException extends BusinessRuleException {
    public EnclosureAlreadyExistsException(String name) {
        super("Enclosure with name '" + name + "' already exists");
    }
}