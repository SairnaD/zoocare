package com.zoo.care.exception;

public class EnclosureFullException extends BusinessRuleException {
    public EnclosureFullException(String enclosureName) {
        super("Enclosure '" + enclosureName + "' is full");
    }
}