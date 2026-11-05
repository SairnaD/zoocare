package com.zoo.care.dto;

import com.zoo.care.entity.User;

public record KeeperResponse(Long id, String username, String fullName) {

    public static KeeperResponse from(User user) {
        return new KeeperResponse(user.getId(), user.getUsername(), user.getFullName());
    }
}