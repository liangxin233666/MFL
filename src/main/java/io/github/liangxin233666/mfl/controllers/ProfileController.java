package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.ProfileResponse;
import io.github.liangxin233666.mfl.services.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProfileResponse profileResponse = profileService.getProfile(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }

    @PostMapping("/follow")
    public ResponseEntity<ProfileResponse> followUser(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProfileResponse profileResponse = profileService.followUser(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }

    @DeleteMapping("/follow")
    public ResponseEntity<ProfileResponse> unfollowUser(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProfileResponse profileResponse = profileService.unfollowUser(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }
}