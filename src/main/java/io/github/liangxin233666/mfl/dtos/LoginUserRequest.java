package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record LoginUserRequest(@Valid @NotNull UserDto user) {
    public record UserDto(
            @NotBlank(message = "email cannot be empty")
            @Email(message = "is not a valid email address")
            String email,

            @NotBlank(message = "password cannot be empty")
            String password
    )
    {  public UserDto {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }}
}