package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record UpdateUserRequest(@Valid @NotNull UserDto user) {
    public record UserDto(
            // Email 可选，但如果存在，则必须是合法的邮箱格式
            @Email(message = "is not a valid email address")
            String email,

            // Password 可选，但如果存在，则长度必须至少为 8
            @Pattern(regexp = "(^$)|(.*\\S+.*)", message = "密码不能只包含空白字符")
            @Size(min = 8, message = "password must be at least 8 characters long")
            String password,

            @Pattern(regexp = ".*\\S+.*", message = "用户名不能只包含空白字符")
            String username,

            @Pattern(regexp = "(^$)|(.*\\S+.*)", message = "描述信息不能只包含空白字符")
            String bio,

            @Pattern(regexp = "(^$)|(.*\\S+.*)", message = "图片url不能只包含空白字符")
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