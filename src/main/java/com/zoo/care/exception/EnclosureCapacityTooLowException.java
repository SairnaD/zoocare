package com.zoo.care.exception;

public class EnclosureCapacityTooLowException extends BusinessRuleException {
    public EnclosureCapacityTooLowException(long animalCount) {
        super("Capacity cannot be lower than the number of animals in the enclosure (" + animalCount + ")");
    }
}