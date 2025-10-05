package io.github.liangxin233666.mfl.services;

import com.github.slugify.Slugify;
import io.github.liangxin233666.mfl.dtos.*;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.TagRepository;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;


@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final Slugify slugify;

    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository, TagRepository tagRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.slugify = Slugify.builder().build();
    }

    @Transactional
    public ArticleResponse createArticle(@Valid NewArticleRequest request, UserDetails currentUserDetails) {

        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));


        Set<Tag> tags = processTagsEfficiently(new HashSet<>(request.article().tagList()));

        Article article = new Article();
        article.setAuthor(author);
        article.setTitle(request.article().title());
        article.setDescription(request.article().description());
        article.setBody(request.article().body());
        article.setTags(tags);

        article.setSlug(generateUniqueSlug(request.article().title()));


        Article savedArticle = articleRepository.save(article);


        return buildArticleResponseSimply(savedArticle);
    }


    private Set<Tag> processTagsEfficiently(Set<String> tagNames) {

        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> existingTags = tagRepository.findByNameIn(tagNames);

        Map<String, Tag> existingTagsMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> tagsToSave = tagNames.stream()
                .filter(name -> !existingTagsMap.containsKey(name))
                .map(name -> {
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return newTag;
                })
                .collect(Collectors.toList());


        if (!tagsToSave.isEmpty()) {

           tagRepository.saveAll(tagsToSave);
        }

        existingTags.addAll(tagsToSave);
        return existingTags;
    }

    private String generateUniqueSlug(String title) {
        int r = ThreadLocalRandom.current().nextInt(100000, 1000000);
        String uniqueSlug= slugify.slugify(title)+"-"+r;

        // 最多一次查询来确认slug是否唯一。只有在发生冲突时才会有额外的查询。
        while (articleRepository.findBySlug(uniqueSlug).isPresent()) {
            int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
            uniqueSlug +=("-" + randomNumber);

        }
        return uniqueSlug;
    }

    //回复详细信息
    private ArticleResponse buildArticleResponse(Article article, User currentUser) {
        List<String> tagList = article.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());

        boolean isFavorited = (currentUser != null) && article.getFavoritedBy().contains(currentUser);
        int favoritesCount = article.getFavoritesCount();

        User author = article.getAuthor();
        boolean isFollowingAuthor = (currentUser != null) && author.getFollowers().contains(currentUser);

        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                author.getUsername(),
                author.getBio(),
                author.getImage(),
                isFollowingAuthor
        );

        ArticleResponse.ArticleDto articleDto = new ArticleResponse.ArticleDto(
                article.getSlug(),
                article.getTitle(),
                article.getDescription(),
                article.getBody(),
                tagList,
                article.getCreatedAt(),
                article.getUpdatedAt(),
                isFavorited,
                favoritesCount,
                authorProfile
        );

        return new ArticleResponse(articleDto);
    }

    //回复大概信息
    private ArticleResponse buildArticleResponseSimply(Article article) {
        User author = article.getAuthor();
        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                author.getUsername(),
                null,
                author.getImage(),
                null
        );

        ArticleResponse.ArticleDto articleDto = new ArticleResponse.ArticleDto(
                article.getSlug(),
                article.getTitle(),
                article.getDescription(),
                null,
                null,
                article.getCreatedAt(),
                article.getUpdatedAt(),
                null,
                null,
                authorProfile
        );

        return new ArticleResponse(articleDto);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new  ResourceNotFoundException("User not found for id: " + id));
    }

    @CacheEvict(value = "articles", key = "#slug")
    @Transactional
    public void deleteArticle(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        // ** 权限校验 **
        if (!article.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this article.");
        }

        articleRepository.delete(article);
    }

    @Transactional
    public ArticleResponse favoriteArticle(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));
        if (!article.getFavoritedBy().contains(currentUser)) {
            article.getFavoritedBy().add(currentUser);
            article.setFavoritesCount(article.getFavoritesCount() + 1);
        }
        return buildArticleResponseSimply(article);
    }

    @Transactional
    public ArticleResponse unfavoriteArticle(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (article.getFavoritedBy().contains(currentUser)) {

            article.getFavoritedBy().remove(currentUser);

            article.setFavoritesCount(Math.max(0, article.getFavoritesCount() - 1));
        }
        return buildArticleResponseSimply(article);
    }

    private Article findArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
    }


    @Cacheable(value = "articles", key = "#slug")
    @Transactional(readOnly = true)
    public ArticleResponse getArticleBySlug(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);

        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        return buildArticleResponse(article, currentUser);
    }

    @CacheEvict(value = "articles", key = "#slug")
    @Transactional
    public ArticleResponse updateArticle(String slug, @Valid UpdateArticleRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));


        if (!article.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this article.");
        }


        //只要传进来的title不为空，slug就会变
        if (request.article().title() != null) {
            article.setTitle(request.article().title());

            article.setSlug(generateUniqueSlug(request.article().title()));
        }

        if (request.article().description() != null) {
            article.setDescription(request.article().description());
        }
        if (request.article().body() != null) {
            article.setBody(request.article().body());
        }

        Article updatedArticle = articleRepository.save(article);
        return buildArticleResponseSimply(updatedArticle);
    }

    //注意，这个是类似b站的返回多篇文章的简要，所以一些信息没有
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getArticles(String tag, String author, String favoritedBy, Pageable pageable, UserDetails currentUserDetails) {

        // 1. 构建动态查询条件 (Specification)
        Specification<Article> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (tag != null && !tag.isBlank()) {
            spec = spec.and(hasTag(tag));
        }
        if (author != null && !author.isBlank()) {
            spec = spec.and(byAuthor(author));
        }
        if (favoritedBy != null && !favoritedBy.isBlank()) {
            spec = spec.and(favoritedBy(favoritedBy));
        }

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);

        List<ArticleResponse.ArticleDto> articleDtos = articlePage.getContent().stream()
                .map(article -> buildArticleResponseSimply(article).article())
                .collect(Collectors.toList());

        return new MultipleArticlesResponse(articleDtos, articlePage.getTotalElements());
    }


    //同时，简要信息
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getFeedArticles(Pageable pageable, UserDetails currentUserDetails) {
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        List<User> followedUsers = List.copyOf(currentUser.getFollowing());
        if (followedUsers.isEmpty()) {

            return new MultipleArticlesResponse(List.of(), 0);
        }

        Page<Article> articlePage = articleRepository.findByAuthorInOrderByCreatedAtDesc(followedUsers, pageable);

        List<ArticleResponse.ArticleDto> articleDtos = articlePage.getContent().stream()
                .map(article -> buildArticleResponseSimply(article).article())
                .collect(Collectors.toList());

        return new MultipleArticlesResponse(articleDtos, articlePage.getTotalElements());
    }

    private Specification<Article> hasTag(String tagName) {
        return (root, query, criteriaBuilder) -> {
            Join<Article, Tag> tagJoin = root.join("tags");
            return criteriaBuilder.equal(tagJoin.get("name"), tagName);
        };
    }

    private Specification<Article> byAuthor(String username) {
        return (root, query, criteriaBuilder) -> {
            Join<Article, User> authorJoin = root.join("author");
            return criteriaBuilder.equal(authorJoin.get("username"), username);
        };
    }

    private Specification<Article> favoritedBy(String username) {
        return (root, query, criteriaBuilder) -> {

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new  ResourceNotFoundException("User not found: " + username));
            Join<Article, User> favoritedByJoin = root.join("favoritedBy");
            return criteriaBuilder.equal(favoritedByJoin.get("id"), user.getId());
        };
    }


}