package io.github.liangxin233666.mfl.dtos;

public record UserResponse(UserDto user) {
    public record UserDto(String email, String token, String username, String bio, String image) {}
}