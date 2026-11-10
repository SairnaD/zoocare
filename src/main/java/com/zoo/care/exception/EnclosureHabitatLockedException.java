package com.zoo.care.exception;

public class EnclosureHabitatLockedException extends BusinessRuleException {
    public EnclosureHabitatLockedException() {
        super("Habitat cannot be changed while the enclosure has animals");
    }
}