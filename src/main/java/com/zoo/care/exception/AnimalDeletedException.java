package com.zoo.care.exception;

public class AnimalDeletedException extends BusinessRuleException {
    public AnimalDeletedException() {
        super("Cannot record a feeding for a deleted animal");
    }
}