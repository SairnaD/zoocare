package com.zoo.care.exception;

public class SpeciesAlreadyExistsException extends BusinessRuleException {
    public SpeciesAlreadyExistsException(String name) {
        super("Species with name '" + name + "' already exists");
    }
}