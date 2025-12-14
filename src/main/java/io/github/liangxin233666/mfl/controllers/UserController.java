package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.*;
import io.github.liangxin233666.mfl.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody NewUserRequest request) {
        UserResponse userResponse = userService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser(@Valid @RequestBody LoginUserRequest request) {
        UserResponse userResponse = userService.loginUser(request);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UpdateUserRequest request,@AuthenticationPrincipal UserDetails currentUser) {
        UserResponse userResponse=userService.updateCurrentUser(request,currentUser);
        return ResponseEntity.ok(userResponse);
    }
    @GetMapping("/following")
    public ResponseEntity<MultipleProfilesResponse> getMyFollowing(
            @PageableDefault Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {
        MultipleProfilesResponse response = userService.getMyFollowing(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers")
    public ResponseEntity<MultipleProfilesResponse> getMyFollowers(
            @PageableDefault Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {
        MultipleProfilesResponse response = userService.getMyFollowers(pageable, currentUser);
        return ResponseEntity.ok(response);
    }
}