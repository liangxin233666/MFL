package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.MultipleArticlesResponse;
import io.github.liangxin233666.mfl.dtos.UpdateUserRequest;
import io.github.liangxin233666.mfl.dtos.UserResponse;
import io.github.liangxin233666.mfl.services.ArticleService;
import io.github.liangxin233666.mfl.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class CurrentUserController {

    private final UserService userService;
    private final ArticleService articleService;

    public CurrentUserController(UserService userService, ArticleService articleService) {
        this.userService = userService;
        this.articleService=articleService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal User currentUser
    )
    {
        UserResponse userResponse = userService.getCurrentUser(currentUser);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserResponse userResponse = userService.updateCurrentUser(request, currentUser);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/feed")
    public ResponseEntity<MultipleArticlesResponse> getArticles(
            @RequestParam(name = "tag", required = false) String tag,

            @RequestParam(name = "author", required = false) String author,

            @RequestParam(name = "favoritedBy", required = false) String favoritedBy,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        MultipleArticlesResponse response = articleService.getArticles(tag, author, favoritedBy,pageable);
        return ResponseEntity.ok(response);
    }
}