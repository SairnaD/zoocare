package com.zoo.care.exception;

public class KeeperAssignmentNotFoundException extends ResourceNotFoundException {
    public KeeperAssignmentNotFoundException(Long enclosureId, Long keeperId) {
        super("Keeper " + keeperId + " is not assigned to enclosure " + enclosureId);
    }
}