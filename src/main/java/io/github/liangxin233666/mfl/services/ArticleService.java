package io.github.liangxin233666.mfl.services;

import com.github.slugify.Slugify;
import io.github.liangxin233666.mfl.dtos.*;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.events.NotificationEvent;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;


@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final Slugify slugify;
    private final FileStorageService fileStorageService;
    private final NotificationProducer notificationProducer;
    private final HistoryService historyService;

    private static final Pattern TEMP_URL_PATTERN = Pattern.compile("https://?[^\\s\"]*/uploads/temp/[^\\s\")]+");

    private static final String URL_REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);


    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository, TagRepository tagRepository, FileStorageService fileStorageService, NotificationProducer notificationProducer,HistoryService  historyService) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.slugify = Slugify.builder().build();
        this.fileStorageService = fileStorageService;
        this.notificationProducer=notificationProducer;
        this.historyService = historyService;
    }

    @Transactional
    public ArticleResponse createArticle(@Valid NewArticleRequest request, UserDetails currentUserDetails) {

        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));


        Set<Tag> tags = processTagsEfficiently(new HashSet<>(request.article().tagList()));

        List<String> tempUrls = new ArrayList<>();
        String coverImageUrl = request.article().coverImageUrl();
        if (coverImageUrl != null) {
            tempUrls.add(coverImageUrl);
        }
        tempUrls.addAll(extractTempUrlsFromMarkdown(request.article().body()));

        // 2. 调用服务进行转正
        Map<String, String> urlMapping = fileStorageService.promoteFiles(tempUrls);

        // 3. 替换文章内容和封面中的URL为永久URL
        String finalBody = replaceUrls(request.article().body(), urlMapping);
        String finalCoverImageUrl = replaceUrls(coverImageUrl, urlMapping);

        Article article = new Article();
        article.setAuthor(author);
        article.setTitle(request.article().title());
        article.setDescription(request.article().description());
        article.setBody(finalBody);
        article.setTags(tags);

        article.setSlug(generateUniqueSlug(request.article().title()));
        article.setCoverImageUrl(finalCoverImageUrl);

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
                article.getCoverImageUrl(),
                authorProfile
        );

        return new ArticleResponse(articleDto);
    }

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
                article.getFavoritesCount(),
                article.getCoverImageUrl(),
                authorProfile
        );

        return new ArticleResponse(articleDto);
    }

    //回复大概信息
    private ArticleResponse buildArticleResponseSimply(Article article,Boolean isFavorited) {
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
                isFavorited,
                article.getFavoritesCount(),
                article.getCoverImageUrl(),
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
            Article savedArticle = articleRepository.save(article);

            // [发送消息]
            notificationProducer.sendNotification(new NotificationEvent(
                    currentUser.getId(),
                    savedArticle.getAuthor().getId(),
                    NotificationEvent.EventType.ARTICLE_LIKED,
                    savedArticle.getId(),
                    savedArticle.getSlug()
            ));
        }

        return buildArticleResponseSimply(article,true);
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



    //@Cacheable(value = "articles", key = "#slug")
    @Transactional(readOnly = true)
    public ArticleResponse getArticleBySlug(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);

        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        if(currentUser != null) {
            historyService.recordHistoryAsync(currentUser, article);
        }

        return buildArticleResponse(article, currentUser);
    }

    @CacheEvict(value = "articles", key = "#slug")
    @Transactional
    public ArticleResponse updateArticle(String slug, UpdateArticleRequest request, UserDetails currentUserDetails) {
        // 1. 查找实体并进行权限校验
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (!article.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this article.");
        }

        // 2. [快照] 保存更新前的所有图片URL，用于后续对比
        List<String> oldUrls = extractAllUrls(
                article.getBody(),
                article.getCoverImageUrl()
        );

        // 3. [转正] 处理新上传的临时图片
        List<String> newTempUrls = extractTempUrls(
                request.article().body(),
                request.article().coverImageUrl()
        );
        Map<String, String> urlMapping = fileStorageService.promoteFiles(newTempUrls);

        // 4. [更新实体] 将请求中的数据更新到article对象
        // a. 处理标题和Slug
        if (request.article().title() != null && !request.article().title().equals(article.getTitle())) {
            article.setTitle(request.article().title());
            article.setSlug(generateUniqueSlug(request.article().title()));
        }
        // b. 处理描述
        if (request.article().description() != null) {
            article.setDescription(request.article().description());
        }
        // c. 处理正文，并将临时URL替换为永久URL
        if (request.article().body() != null) {
            String finalBody = replaceUrls(request.article().body(), urlMapping);
            article.setBody(finalBody);
        }
        // d. 处理封面图，并将临时URL替换为永久URL
        if (request.article().coverImageUrl() != null) {
            String finalCoverUrl = replaceUrls(request.article().coverImageUrl(), urlMapping);
            article.setCoverImageUrl(finalCoverUrl);
        }

        // 5. [保存] 将更新后的文章保存到数据库
        Article updatedArticle = articleRepository.save(article);

        // 6. [对比与清理] 计算出被删除的URL，并提交异步删除任务
        List<String> newUrls = extractAllUrls(
                updatedArticle.getBody(),
                updatedArticle.getCoverImageUrl()
        );

        oldUrls.removeAll(newUrls); // 计算差集，剩下的是需要被删除的孤儿URL

        if (!oldUrls.isEmpty()) {
            fileStorageService.deleteFilesAsync(oldUrls);
        }

        // 7. [返回] 构建并返回最终的响应
        return buildArticleResponse(updatedArticle, currentUser);
    }

    //注意，这个是类似b站的返回多篇文章的简要，所以一些信息没有
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getArticles(String tag, String author, String favoritedBy, Pageable pageable) {

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

            Join<Article, User> favoritedByJoin = root.join("favoritedBy");


            return criteriaBuilder.equal(favoritedByJoin.get("username"), username);
        };
    }

    private Specification<Article> favoriting(UserDetails currentUserDetails) {
        return (root, query, criteriaBuilder) -> {

            Join<Article, User> favoritedByJoin = root.join("favoritedBy");

            return criteriaBuilder.equal(favoritedByJoin.get("id"), currentUserDetails.getUsername());
        };
    }

    private List<String> extractTempUrlsFromMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tempUrls = new ArrayList<>();
        Matcher matcher = TEMP_URL_PATTERN.matcher(markdown);
        while (matcher.find()) {
            tempUrls.add(matcher.group());
        }
        return tempUrls;
    }

    private String replaceUrls(String text, Map<String, String> urlMapping) {
        if (text == null || urlMapping == null || urlMapping.isEmpty()) {
            return text;
        }
        for (Map.Entry<String, String> entry : urlMapping.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private List<String> extractAllUrls(String body, String coverUrl) {
        return extractUrls(body, coverUrl);
    }

    private List<String> extractUrls(String... texts) {
        if (texts == null || texts.length == 0) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (String text : texts) {
            if(text == null || text.isEmpty()) continue;
            Matcher matcher = URL_PATTERN.matcher(text);
            while (matcher.find()) {
                urls.add(matcher.group());
            }
        }
        return urls;
    }
    private List<String> extractTempUrls(String body, String coverUrl) {
        return extractUrls(body, coverUrl).stream()
                .filter(url -> url.contains("/uploads/temp/"))
                .collect(Collectors.toList());
    }

}