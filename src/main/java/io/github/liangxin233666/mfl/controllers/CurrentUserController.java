package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.UserResponse;
import io.github.liangxin233666.mfl.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class CurrentUserController {

    private final UserService userService;

    public CurrentUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(

            @AuthenticationPrincipal User currentUser
    ) {
        UserResponse userResponse = userService.getCurrentUser(currentUser);
        return ResponseEntity.ok(userResponse);
    }
}