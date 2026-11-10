package com.zoo.care.exception;

public class EnclosureNotEmptyException extends BusinessRuleException {
    public EnclosureNotEmptyException() {
        super("Enclosure still has animals and cannot be deleted");
    }
}