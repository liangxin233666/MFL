package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@Valid @NotNull UserDto user) {
    public record UserDto(
            // Email 可选，但如果存在，则必须是合法的邮箱格式
            @Email(message = "is not a valid email address")
            String email,

            // Password 可选，但如果存在，则长度必须至少为 8
            @Size(min = 8, message = "password must be at least 8 characters long")
            String password,

            @NotBlank(message = "Username can't be blank")
            String username,

            String bio,
            String image
    )
    { public UserDto
        {
            if (username != null) username = username.trim();
            if (email != null) email = email.trim().toLowerCase();
            if (bio != null) bio = bio.trim();
            if (image != null) image = image.trim();
        }
    }
}