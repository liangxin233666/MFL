package io.github.liangxin233666.mfl.dtos;

public record NewUserRequest(UserDto user) {
    public record UserDto(String username, String email, String password) {}
}