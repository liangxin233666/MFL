package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record NewUserRequest(@Valid @NotNull UserDto user) {
    public record UserDto(
            @NotBlank(message = "Username can't be blank")
            String username,

            @NotBlank(message = "Email can't be blank")
            @Email(message = "Should be a valid email format")
            String email,

            @NotBlank(message = "Password can't be blank")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {

        public UserDto {
            if (username != null) {
                username = username.trim();
            }
            if (email != null) {
                email = email.trim().toLowerCase();
            }
        }
    }
}