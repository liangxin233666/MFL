package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.TagsResponse;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.repositories.TagRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Cacheable("tags")
    public TagsResponse getAllTags() {
        List<String> tags = tagRepository.findAll().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        return new TagsResponse(tags);
    }
}