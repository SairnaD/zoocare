package com.zoo.care.exception;

public class SpeciesInUseException extends BusinessRuleException {
    public SpeciesInUseException() {
        super("Species is in use and cannot be deleted");
    }
}