package io.github.liangxin233666.mfl.dtos;

public record ProfileResponse(ProfileDto profile) {
    public record ProfileDto(String username, String bio, String image, Boolean following) {}
}