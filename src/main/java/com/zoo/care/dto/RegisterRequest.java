package com.zoo.care.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @NotBlank
        @Size(max = 100)
        String fullName
) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username
                + ", email=" + email
                + ", fullName=" + fullName + "]";
    }
}