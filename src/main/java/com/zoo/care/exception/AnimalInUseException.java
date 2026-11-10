package com.zoo.care.exception;

public class AnimalInUseException extends BusinessRuleException {
    public AnimalInUseException() {
        super("Animal has related records (for example feedings) and cannot be deleted");
    }
}