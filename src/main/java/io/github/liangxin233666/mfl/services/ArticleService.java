package io.github.liangxin233666.mfl.services;

import com.github.slugify.Slugify;
import io.github.liangxin233666.mfl.config.RabbitMqAuditConfig;
import io.github.liangxin233666.mfl.dtos.*;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.TagRepository;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import io.github.liangxin233666.mfl.repositories.es.EsArticleRepository;
import io.github.liangxin233666.mfl.repositories.projections.ArticleSimpleView;
import jakarta.persistence.criteria.Join;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final Slugify slugify;
    private final FileStorageService fileStorageService;
    private final NotificationProducer notificationProducer;
    private final HistoryService historyService;
    private final RabbitTemplate rabbitTemplate;
    private final EsArticleRepository esArticleRepository;
    private final UserService userService;
    private final RecommendationService recommendationService;
    private final GlobalTrendManager globalTrendManager;


    private static final Pattern TEMP_URL_PATTERN = Pattern.compile("https://?[^\\s\"]*/uploads/temp/[^\\s\")]+");
    private static final String URL_REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);


    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository, TagRepository tagRepository, FileStorageService fileStorageService, NotificationProducer notificationProducer, HistoryService historyService, RabbitTemplate rabbitTemplate, EsArticleRepository esArticleRepository, UserService userService, RecommendationService recommendationService, GlobalTrendManager globalTrendManager) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.slugify = Slugify.builder().build();
        this.fileStorageService = fileStorageService;
        this.notificationProducer = notificationProducer;
        this.historyService = historyService;
        this.rabbitTemplate = rabbitTemplate;
        this.esArticleRepository = esArticleRepository;
        this.userService = userService;
        this.recommendationService = recommendationService;
        this.globalTrendManager = globalTrendManager;
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

        rabbitTemplate.convertAndSend(RabbitMqAuditConfig.AUDIT_QUEUE, savedArticle.getId());

        log.info(savedArticle.getId().toString());

        // 写操作虽然返回简单视图，但手头已经有Entity，直接复用Entity版
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
        String uniqueSlug = slugify.slugify(title) + "-" + r;
        while (articleRepository.findBySlug(uniqueSlug).isPresent()) {
            int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
            uniqueSlug += ("-" + randomNumber);
        }
        return uniqueSlug;
    }

    // 详细信息（Get Single）- 仍需 Body，保持原样
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

    // --- 保留Entity版本供 write操作复用 ---
    private ArticleResponse buildArticleResponseSimply(Article article) {
        return buildArticleResponseSimply(article, null);
    }

    // --- 保留Entity版本供 write操作复用 (带 favorited 参数) ---
    private ArticleResponse buildArticleResponseSimply(Article article, Boolean isFavorited) {
        User author = article.getAuthor();
        return getArticleResponse(isFavorited, author.getUsername(), author.getImage(), article.getSlug(), article.getTitle(), article.getDescription(), article.getCreatedAt(), article.getUpdatedAt(), article.getFavoritesCount(), article.getCoverImageUrl());
    }

    // ==========================================
    // NEW: 核心改造点 - 投影版本的 buildResponse
    // ==========================================
    private ArticleResponse buildArticleResponseSimply(ArticleSimpleView view, Boolean isFavorited) {
        // 从投影的 Nested Interface 获取 Author 信息
        ArticleSimpleView.AuthorView authorView = view.getAuthor();
        return getArticleResponse(isFavorited, authorView.getUsername(), authorView.getImage(), view.getSlug(), view.getTitle(), view.getDescription(), view.getCreatedAt(), view.getUpdatedAt(), view.getFavoritesCount(), view.getCoverImageUrl());
    }

    private ArticleResponse getArticleResponse(Boolean isFavorited, String username, String image, String slug, String title, String description, OffsetDateTime createdAt, OffsetDateTime updatedAt, Integer favoritesCount, String coverImageUrl) {
        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                username,
                null,
                image,
                null
        );

        ArticleResponse.ArticleDto articleDto = new ArticleResponse.ArticleDto(
                slug,
                title,
                description,
                null, // 投影压根没查 Body，所以为 null，极致性能
                null,
                createdAt,
                updatedAt,
                isFavorited,
                favoritesCount,
                coverImageUrl,
                authorProfile
        );

        return new ArticleResponse(articleDto);
    }


    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found for id: " + id));
    }

    @CacheEvict(value = "articles", key = "#slug")
    @Transactional
    public void deleteArticle(String slug, UserDetails currentUserDetails) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));

        User currentUser = userRepository.findById(Long.valueOf(currentUserDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = currentUserDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!article.getAuthor().equals(currentUser) && !isAdmin) {
            throw new AccessDeniedException("You are not the author of this article.");
        }

        List<String> filesToDelete = new ArrayList<>();
        if (article.getCoverImageUrl() != null) {
            filesToDelete.add(article.getCoverImageUrl());
        }
        filesToDelete.addAll(extractUrls(article.getBody(), article.getCoverImageUrl()));

        articleRepository.delete(article);

        try {
            esArticleRepository.deleteById(article.getId());
        } catch (Exception e) {
            log.error("Failed to delete article index from ES: {}", article.getId(), e);
        }

        fileStorageService.deleteFilesAsync(filesToDelete);
    }

    // Write 操作仍保持 Entity，因为我们改变了关系状态
    @Transactional
    public ArticleResponse favoriteArticle(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (!article.getFavoritedBy().contains(currentUser)) {
            article.getFavoritedBy().add(currentUser);
            article.setFavoritesCount(article.getFavoritesCount() + 1);
            Article savedArticle = articleRepository.save(article);

            notificationProducer.sendNotification(new NotificationEvent(
                    currentUser.getId(),
                    savedArticle.getAuthor().getId(),
                    NotificationEvent.EventType.ARTICLE_LIKED,
                    savedArticle.getId(),
                    savedArticle.getSlug(),
                    null
            ));
        }
        // 返回时带上确定的true
        return buildArticleResponseSimply(article, true);
    }

    @Transactional
    public ArticleResponse unfavoriteArticle(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (article.getFavoritedBy().contains(currentUser)) {
            article.getFavoritedBy().remove(currentUser);
            article.setFavoritesCount(Math.max(0, article.getFavoritesCount() - 1));
        }
        return buildArticleResponseSimply(article, false);
    }

    private Article findArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
    }

    // ==============================================================
    // 改动 1: searchArticles
    // 逻辑：ES出ID -> ID出投影 -> 重组顺序 -> 批量查收藏状态 -> 构建Response
    // ==============================================================
    @Transactional(readOnly = true)
    public MultipleArticlesResponse searchArticles(String query, Pageable pageable, UserDetails currentUserDetails) {
        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        // 1. ES 查询获取相关性排序后的 Page
        Page<ArticleDocument> esPage = esArticleRepository.searchIdeally(query, pageable);

        if (esPage.isEmpty()) {
            return new MultipleArticlesResponse(List.of(), 0);
        }

        // 2. 提取 ID 列表 (有序)
        List<Long> articleIds = esPage.getContent().stream()
                .map(ArticleDocument::getId)
                .collect(Collectors.toList());

        // 3. 特殊查找方法：只查 Projection，不查 Body
        // 4. 重组逻辑 (抽取通用方法处理)
        List<ArticleResponse.ArticleDto> sortedDtos = fetchViewsByIdsAndPreserveOrder(articleIds, currentUser);

        return new MultipleArticlesResponse(sortedDtos, esPage.getTotalElements());
    }

    @Transactional
    public ArticleResponse getArticleBySlug(String slug, UserDetails currentUserDetails) {
        // 这里必须用 findBySlug (Entity) 因为 getArticle 详情页通常需要 Body
        Article article = findArticleBySlug(slug);

        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;
        boolean isAuthor = article.getAuthor().equals(currentUser);
        boolean isPublished = (article.getStatus() == Article.ArticleStatus.PUBLISHED);

        if (!isPublished && !isAuthor) {
            throw new ResourceNotFoundException("Article not found");
        }
        if (currentUser != null) {
            historyService.recordHistoryAsync(currentUser, article);
            updateUserInterestAsync(currentUser, article.getId());
        }

        return buildArticleResponse(article, currentUser);
    }

    @CacheEvict(value = "articles", key = "#slug")
    @Transactional
    public ArticleResponse updateArticle(String slug, UpdateArticleRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (!article.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this article.");
        }

        List<String> oldUrls = extractAllUrls(article.getBody(), article.getCoverImageUrl());

        List<String> newTempUrls = extractTempUrls(
                request.article().body(),
                request.article().coverImageUrl()
        );
        Map<String, String> urlMapping = fileStorageService.promoteFiles(newTempUrls);

        if (request.article().title() != null && !request.article().title().equals(article.getTitle())) {
            article.setTitle(request.article().title());
            article.setSlug(generateUniqueSlug(request.article().title()));
        }
        if (request.article().description() != null) {
            article.setDescription(request.article().description());
        }
        if (request.article().body() != null) {
            String finalBody = replaceUrls(request.article().body(), urlMapping);
            article.setBody(finalBody);
        }
        if (request.article().coverImageUrl() != null) {
            String finalCoverUrl = replaceUrls(request.article().coverImageUrl(), urlMapping);
            article.setCoverImageUrl(finalCoverUrl);
        }

        Article updatedArticle = articleRepository.save(article);

        List<String> newUrls = extractAllUrls(updatedArticle.getBody(), updatedArticle.getCoverImageUrl());
        oldUrls.removeAll(newUrls);

        if (!oldUrls.isEmpty()) {
            fileStorageService.deleteFilesAsync(oldUrls);
        }

        return buildArticleResponse(updatedArticle, currentUser);
    }

    // ==============================================================
    // 改动 2: getArticles
    // 逻辑：Specification 查找 -> 投影化 (Spring Data JPA Fluent API)
    //       -> 批量查点赞状态 -> 构建 SimpleResponse
    // ==============================================================
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getArticles(String tag, String author, String favoritedBy, Pageable pageable) {
        Specification<Article> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        spec = spec.and(statusIsPublished());
        if (tag != null && !tag.isBlank()) {
            spec = spec.and(hasTag(tag));
        }
        if (author != null && !author.isBlank()) {
            spec = spec.and(byAuthor(author));
        }
        if (favoritedBy != null && !favoritedBy.isBlank()) {
            spec = spec.and(favoritedBy(favoritedBy));
        }

        // --- NEW: 使用 repository.findBy + 投影转换，而不是 findAll ---
        // 这样数据库层面就只会 Select ID, Title, Description, Image... 忽略 Body
        Page<ArticleSimpleView> viewPage = articleRepository.findBy(spec, q -> q.as(ArticleSimpleView.class).page(pageable));

        if (viewPage.isEmpty()) {
            return new MultipleArticlesResponse(List.of(), 0);
        }

        // 获取当前用户上下文用于查点赞
        // 注意：Controller 这一层没传 currentUser 进这个方法，这里假设如果是公共接口不传user，或者 SecurityContextHolder 自行获取
        // 为了安全起见，我们查看 getFeed 等都有 user，这里如果没有传，我们可以尝试获取，或默认为空
        // (修正: 保持参数一致性，这里假设是匿名或由外部传入，若Service签名不可变且未传入user，则默认 isFavorited=false)
        // 假设通过 Security Context 拿
        Long currentUserId = null;
        try {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (!"anonymousUser".equals(username)) {
                currentUserId = Long.valueOf(username);
            }
        } catch (Exception ignored) {}

        // 批量查询点赞状态
        Set<Long> likedIds = (currentUserId == null)
                ? Collections.emptySet()
                : checkLikedIds(viewPage.getContent().stream().map(ArticleSimpleView::getId).toList(), currentUserId);

        List<ArticleResponse.ArticleDto> articleDtos = viewPage.getContent().stream()
                .map(view -> buildArticleResponseSimply(view, likedIds.contains(view.getId())).article())
                .collect(Collectors.toList());

        return new MultipleArticlesResponse(articleDtos, viewPage.getTotalElements());
    }


    // ==============================================================
    // 改动 3: getFeedArticles
    // 逻辑：查找 Repository 定义的投影方法
    // ==============================================================
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getFeedArticles(Pageable pageable, UserDetails currentUserDetails) {
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        List<User> followedUsers = List.copyOf(currentUser.getFollowing());
        if (followedUsers.isEmpty()) {
            return new MultipleArticlesResponse(List.of(), 0);
        }

        // --- NEW: 调用 Repository 中返回 Page<ArticleSimpleView> 的方法 ---
        Page<ArticleSimpleView> viewPage = articleRepository.findByAuthorInAndStatusOrderByCreatedAtDesc(
                followedUsers,
                Article.ArticleStatus.PUBLISHED,
                pageable
        );

        if (viewPage.isEmpty()) {
            return new MultipleArticlesResponse(List.of(), 0);
        }

        // 批量查点赞
        Set<Long> likedIds = checkLikedIds(viewPage.getContent().stream().map(ArticleSimpleView::getId).toList(), currentUser.getId());

        List<ArticleResponse.ArticleDto> articleDtos = viewPage.getContent().stream()
                .map(view -> buildArticleResponseSimply(view, likedIds.contains(view.getId())).article())
                .collect(Collectors.toList());

        return new MultipleArticlesResponse(articleDtos, viewPage.getTotalElements());
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

    private Specification<Article> statusIsPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), Article.ArticleStatus.PUBLISHED);
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
            if (text == null || text.isEmpty()) continue;
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


    @Async
    public void updateUserInterestAsync(User user, Long articleId) {
        ArticleDocument doc = esArticleRepository.findById(articleId).orElse(null);
        if (user != null && doc != null && doc.getEmbeddingVector() != null) {
            String newVecStr = userService.updateUserEmbedding(user.getEmbeddingVector(), doc.getEmbeddingVector());
            user.setEmbeddingVector(newVecStr);
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public MultipleArticlesResponse getRecommendedFeed(UserDetails currentUserDetails) {
        User user = currentUserDetails == null ? null : findUserById(Long.valueOf(currentUserDetails.getUsername()));

        // 注意：getArticles 我们也已经改成了使用 Projection 的版本，所以冷启动这里的逻辑是复用了优化
        if (user == null) return getArticles(null, null, null, PageRequest.of(0, 16));

        float[] userVector = parseEmbeddingString(user.getEmbeddingVector());

        if (userVector == null) {
            return getArticles(null, null, null, PageRequest.of(0, 16));
        }

        List<Long> ids = recommendationService.recommendForUser(userVector, 16);

        // --- NEW: 使用通用方法 fetching Projections 而非 Entity ---
        List<ArticleResponse.ArticleDto> dtos = fetchViewsByIdsAndPreserveOrder(ids, user);

        return new MultipleArticlesResponse(dtos, dtos.size());
    }

    @Transactional(readOnly = true)
    public MultipleArticlesResponse getRelatedArticles(String currentArticleSlug, UserDetails currentUserDetails) {
        User user = findUserById(Long.valueOf(currentUserDetails.getUsername()));
        ArticleDocument sourceDoc = esArticleRepository.findBySlug(currentArticleSlug);

        if (sourceDoc == null || sourceDoc.getEmbeddingVector() == null) {
            return new MultipleArticlesResponse(Collections.emptyList(), 0);
        }

        List<Long> ids = recommendationService.getRelatedArticles(sourceDoc, 6);

        // --- NEW: 使用通用方法 fetching Projections 而非 Entity ---
        List<ArticleResponse.ArticleDto> dtos = fetchViewsByIdsAndPreserveOrder(ids, user);

        return new MultipleArticlesResponse(dtos, dtos.size());
    }

    // --- Helper 1: 按ID列表顺序 获取 Projections 并 填充点赞状态 ---
    // 这个方法非常重要，处理了 Recommendation 和 Search 的优化逻辑
    private List<ArticleResponse.ArticleDto> fetchViewsByIdsAndPreserveOrder(List<Long> ids, User currentUser) {
        if (ids.isEmpty()) return Collections.emptyList();

        // 1. 特殊查找方法：findProjectedByIdIn (Repository需支持)
        List<ArticleSimpleView> views = articleRepository.findProjectedByIdIn(ids);

        // 2. 转 Map 方便 O(1) 取用
        Map<Long, ArticleSimpleView> map = views.stream()
                .collect(Collectors.toMap(ArticleSimpleView::getId, Function.identity()));

        // 3. 批量查当前用户的点赞状态 (解决 N+1 问题)
        Set<Long> likedIds = (currentUser == null) ? Collections.emptySet()
                : checkLikedIds(ids, currentUser.getId());

        // 4. 重组顺序，转换为 Response DTO
        return ids.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(view -> buildArticleResponseSimply(view, likedIds.contains(view.getId())).article())
                .collect(Collectors.toList());
    }

    // --- Helper 2: 封装批量查点赞的 Repository 调用 ---
    private Set<Long> checkLikedIds(List<Long> articleIds, Long userId) {
        if (articleIds.isEmpty() || userId == null) return Collections.emptySet();
        return articleRepository.findLikedArticleIds(userId, articleIds);
    }

    private float[] parseEmbeddingString(String str) {
        if (str == null || str.isBlank()) return null;
        String[] parts = str.split(",");
        float[] res = new float[parts.length];
        for (int i = 0; i < parts.length; i++) res[i] = Float.parseFloat(parts[i]);
        return res;
    }

}