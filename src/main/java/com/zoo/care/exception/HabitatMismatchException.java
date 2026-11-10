package com.zoo.care.exception;

public class HabitatMismatchException extends BusinessRuleException {
    public HabitatMismatchException(String speciesName, String enclosureName) {
        super("Habitat of species '" + speciesName + "' does not match enclosure '" + enclosureName + "'");
    }
}