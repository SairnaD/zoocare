package com.zoo.care.exception;

public class UserIsNotKeeperException extends BusinessRuleException {
    public UserIsNotKeeperException() {
        super("Only users with role KEEPER can be assigned to an enclosure");
    }
}