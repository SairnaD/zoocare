package com.zoo.care.exception;

public class SameEnclosureException extends BusinessRuleException {
    public SameEnclosureException() {
        super("Animal is already in this enclosure");
    }
}