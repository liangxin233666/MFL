package io.github.liangxin233666.mfl.dtos;

import java.util.List;

public record MultipleProfilesResponse(List<ProfileResponse.ProfileDto> profiles, long profilesCount) {
}