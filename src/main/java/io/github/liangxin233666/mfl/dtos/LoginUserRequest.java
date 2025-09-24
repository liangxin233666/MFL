package io.github.liangxin233666.mfl.dtos;

public record LoginUserRequest(UserDto user) {
    public record UserDto(String email, String password) {}
}