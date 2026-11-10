package com.zoo.care.exception;

public class SpeciesHabitatLockedException extends BusinessRuleException {
    public SpeciesHabitatLockedException() {
        super("Habitat cannot be changed while the species has animals");
    }
}