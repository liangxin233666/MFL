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
import jakarta.persistence.criteria.Join;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
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
    private final RabbitTemplate rabbitTemplate;
    private final EsArticleRepository esArticleRepository;
    private final UserService userService;
    private final RecommendationService recommendationService;


    private static final Pattern TEMP_URL_PATTERN = Pattern.compile("https://?[^\\s\"]*/uploads/temp/[^\\s\")]+");

    private static final String URL_REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);


    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository, TagRepository tagRepository, FileStorageService fileStorageService, NotificationProducer notificationProducer,HistoryService  historyService,RabbitTemplate rabbitTemplate, EsArticleRepository esArticleRepository, UserService userService, RecommendationService recommendationService) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.slugify = Slugify.builder().build();
        this.fileStorageService = fileStorageService;
        this.notificationProducer=notificationProducer;
        this.historyService = historyService;
        this.rabbitTemplate = rabbitTemplate;
        this.esArticleRepository = esArticleRepository;
        this.userService = userService;
        this.recommendationService = recommendationService;
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
                    savedArticle.getSlug(),
                    null
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

    @Transactional(readOnly = true)
    public MultipleArticlesResponse searchArticles(String query, Pageable pageable, UserDetails currentUserDetails) {
        // 1. 获取当前用户 (用于判断 isFavorited/isFollowing)
        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        // 2. [ES 查询] 在 ES 索引中查找 ID 列表
        // ES 会对 Title, Description, AiKeywords 三个字段同时检索
        Page<ArticleDocument> esPage = esArticleRepository.searchIdeally(query, pageable);

        if (esPage.isEmpty()) {
            return new MultipleArticlesResponse(List.of(), 0);
        }

        // 3. [ID 提取]
        List<Long> articleIds = esPage.getContent().stream()
                .map(ArticleDocument::getId)
                .collect(Collectors.toList());

        // 4. [DB 回捞] 批量查数据库获取完整实体
        List<Article> dbArticles = articleRepository.findAllById(articleIds);

        // 5. [内存重组] 关键步骤：ES 返回的顺序是按相关度排序的（Rank），而 DB 的 findAllById 不保证顺序。
        //    所以我们需要手动按 articleIds 的顺序，重新把 dbArticles 排好。

        //    技巧：先转成 Map<ID, Article> 方便查找
        Map<Long, Article> articleMap = dbArticles.stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        List<ArticleResponse.ArticleDto> sortedDtos = articleIds.stream()
                .map(articleMap::get) // 按 ES 给的 ID 顺序取对象
                .filter(Objects::nonNull) // 防御性编程：理论上不会空，除非数据不同步
                .map(article -> buildArticleResponseSimply(article).article()) // 转换为 DTO
                .collect(Collectors.toList());
        return new MultipleArticlesResponse(sortedDtos, esPage.getTotalElements());
    }

    //@Cacheable(value = "articles", key = "#slug")
    @Transactional
    public ArticleResponse getArticleBySlug(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);

        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;
        boolean isAuthor = article.getAuthor().equals(currentUser);
        boolean isPublished = (article.getStatus() == Article.ArticleStatus.PUBLISHED);

        if (!isPublished && !isAuthor) {
            // 如果既不是已发布，看的人也不是作者本人 -> 404 或者 403
            // 行业惯例通常报 404，装作这篇文章不存在，防止别人套取信息
            throw new ResourceNotFoundException("Article not found");
        }
        if(currentUser != null) {
            historyService.recordHistoryAsync(currentUser, article);
            updateUserInterestAsync(currentUser,article.getId());
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

        Page<Article> articlePage = articleRepository.findByAuthorInAndStatusOrderByCreatedAtDesc(
                followedUsers,
                Article.ArticleStatus.PUBLISHED, // <--- 关键参数
                pageable
        );

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

    private Specification<Article> statusIsPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), Article.ArticleStatus.PUBLISHED);
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


    @Async // 异步更新画像，不阻塞
    public void updateUserInterestAsync(User user, Long articleId) {
        // 从 ES 捞出文章的向量 (因为 DB 里没存向量，ES 查得快)
        ArticleDocument doc = esArticleRepository.findById(articleId).orElse(null);

        if (user != null && doc != null && doc.getEmbeddingVector() != null) {
            String newVecStr = userService.updateUserEmbedding(user.getEmbeddingVector(), doc.getEmbeddingVector());
            user.setEmbeddingVector(newVecStr);
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public MultipleArticlesResponse getRecommendedFeed(UserDetails currentUserDetails) {
        // 1. 捞用户画像 (从 DB/Redis)
        User user = currentUserDetails==null?null:findUserById(Long.valueOf(currentUserDetails.getUsername()));
        if (user == null) return getArticles(null,null,null, PageRequest.of(0, 16));

        // 2. 解析 Vector 字符串 -> float[]
        float[] userVector = parseEmbeddingString(user.getEmbeddingVector());

        // 3. 冷启动处理 (如果是新用户，没向量) -> 返回最新发布文章
        if (userVector == null) {

            return getArticles(null,null,null,PageRequest.of(0, 16));
        }

        // 4. 调用 ES 推荐算法 (拿回 ID 列表)
        List<Long> ids = recommendationService.recommendForUser(userVector, 16);

        // 5. 根据 ID 回捞数据并组装
        List<ArticleResponse.ArticleDto> dtos = fetchArticlesByIdsAndPreserveOrder(ids, user);

        return new MultipleArticlesResponse(dtos, dtos.size());
    }

    // ---------------------------------------------
    // 函数 2: 获取相关文章 (看了又看)
    // ---------------------------------------------
    @Transactional(readOnly = true)
    public MultipleArticlesResponse getRelatedArticles(String currentArticleSlug, UserDetails currentUserDetails) {
        User user = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        // 1. 先去 ES 找当前这篇文章的 Document (目的是拿到它的向量)
        ArticleDocument sourceDoc = esArticleRepository.findBySlug(currentArticleSlug);

        if (sourceDoc == null || sourceDoc.getEmbeddingVector() == null) {
            return new MultipleArticlesResponse(Collections.emptyList(), 0);
        }

        // 2. 调用 ES 混合检索 (拿回 ID 列表)
        List<Long> ids = recommendationService.getRelatedArticles(sourceDoc, 6);

        // 3. 组装数据
        List<ArticleResponse.ArticleDto> dtos = fetchArticlesByIdsAndPreserveOrder(ids, user);

        return new MultipleArticlesResponse(dtos, dtos.size());
    }

    // --- 内部通用私有方法：按ID取文章并保持 ES 给的顺序 ---
    private List<ArticleResponse.ArticleDto> fetchArticlesByIdsAndPreserveOrder(List<Long> ids, User currentUser) {
        if (ids.isEmpty()) return Collections.emptyList();

        // MySql IN Query
        List<Article> articles = articleRepository.findAllById(ids);

        // List -> Map 为了O(1)查找
        Map<Long, Article> map = articles.stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        // 按 ids 的顺序（因为ids的顺序就是推荐分数的顺序）重组 List
        return ids.stream()
                .map(map::get)
                .filter(Objects::nonNull) // 防御性判空
                .map(article -> buildArticleResponse(article, currentUser).article())
                .collect(Collectors.toList());
    }

    // 工具: String "0.1,0.2..." -> float[]
    private float[] parseEmbeddingString(String str) {
        if (str == null || str.isBlank()) return null;
        String[] parts = str.split(",");
        float[] res = new float[parts.length];
        for (int i = 0; i < parts.length; i++) res[i] = Float.parseFloat(parts[i]);
        return res;
    }

}