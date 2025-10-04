package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.TagsResponse;
import io.github.liangxin233666.mfl.services.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<TagsResponse> getTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }
}