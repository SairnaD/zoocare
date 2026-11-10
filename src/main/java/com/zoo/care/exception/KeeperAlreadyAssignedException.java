package com.zoo.care.exception;

public class KeeperAlreadyAssignedException extends BusinessRuleException {
    public KeeperAlreadyAssignedException() {
        super("Keeper is already assigned to this enclosure");
    }
}