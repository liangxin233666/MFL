package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.*;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import io.github.liangxin233666.mfl.repositories.projections.UserSimpleView;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    private final GlobalTrendManager globalTrendManager;


    // 使用构造器注入所有依赖
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, FileStorageService fileStorageService, GlobalTrendManager globalTrendManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.fileStorageService = fileStorageService;
        this.globalTrendManager = globalTrendManager;
    }

    @Transactional
    public UserResponse registerNewUser(@Valid NewUserRequest request) {
        // 检查用户名和邮箱是否已存在
        if (userRepository.findByUsername(request.user().username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.user().email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User newUser = new User();
        newUser.setUsername(request.user().username());
        newUser.setEmail(request.user().email());
        // 使用BCrypt加密密码
        newUser.setPassword(passwordEncoder.encode(request.user().password()));
        newUser.setEmbeddingVector(serializeEmbedding(globalTrendManager.getGlobalHotVector()));

        User savedUser = userRepository.save(newUser);

        // 生成JWT
        String token = jwtService.generateToken(savedUser);

        // 构建并返回符合API规范的响应
        return buildUserResponse(savedUser, token);
    }

    public UserResponse loginUser(@Valid LoginUserRequest request) {
        User user = userRepository.findByEmail(request.user().email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.user().password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return buildUserResponse(user, token);
    }

    // 一个辅助方法，用于构建UserResponse
    private UserResponse buildUserResponse(User user, String token) {
        UserResponse.UserDto userDto = new UserResponse.UserDto(
                user.getEmail(),
                token,
                user.getUsername(),
                user.getBio(),
                user.getImage()
        );
        return new UserResponse(userDto);
    }

    @Cacheable(value = "users-current", key = "#currentUserDetails.username")
    public UserResponse getCurrentUser( UserDetails currentUserDetails) {

        Long currentUserId = Long.valueOf(currentUserDetails.getUsername());

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return buildUserResponse(user, token);
    }

    @Transactional(readOnly = true)
    public MultipleProfilesResponse getMyFollowing(Pageable pageable, UserDetails currentUserDetails) {
        Long currentUserId = Long.valueOf(currentUserDetails.getUsername());

        // 查询我关注的人
        Page<UserSimpleView> page = userRepository.findFollowingByUserId(currentUserId, pageable);

        List<ProfileResponse.ProfileDto> profiles = page.getContent().stream()
                .map(view -> new ProfileResponse.ProfileDto(
                        view.getUsername(),
                        null, // bio 设为 null
                        view.getImage(),
                        true // 既然在"我关注的人"列表中，following 必然为 true
                ))
                .collect(Collectors.toList());

        return new MultipleProfilesResponse(profiles, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public MultipleProfilesResponse getMyFollowers(Pageable pageable, UserDetails currentUserDetails) {
        Long currentUserId = Long.valueOf(currentUserDetails.getUsername());

        // 查询关注我的人 (粉丝)
        Page<UserSimpleView> page = userRepository.findFollowersByUserId(currentUserId, pageable);

        if (page.isEmpty()) {
            return new MultipleProfilesResponse(Collections.emptyList(), 0);
        }

        // 提取粉丝的 ID 列表
        List<Long> followerIds = page.getContent().stream()
                .map(UserSimpleView::getId)
                .toList();

        // 批量检查我是否关注了这些粉丝 (是否互关)
        Set<Long> myFollowingIds = userRepository.checkFollowingStatus(currentUserId, followerIds);

        List<ProfileResponse.ProfileDto> profiles = page.getContent().stream()
                .map(view -> new ProfileResponse.ProfileDto(
                        view.getUsername(),
                        null, // bio 设为 null
                        view.getImage(),
                        myFollowingIds.contains(view.getId()) // 根据查询结果设置 true/false
                ))
                .collect(Collectors.toList());

        return new MultipleProfilesResponse(profiles, page.getTotalElements());
    }


    @Transactional
    @CacheEvict(cacheNames = {"profiles", "user-details", "users-current"}, key = "#currentUserDetails.username")
    public UserResponse updateCurrentUser(@Valid UpdateUserRequest request, UserDetails currentUserDetails) {
        User userToUpdate = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        UpdateUserRequest.UserDto updates = request.user();


        if (updates.email() != null) {

            if (!updates.email().equals(userToUpdate.getEmail()) && userRepository.findByEmail(updates.email()).isPresent()) {
                throw new IllegalArgumentException("Email already in use");
            }
            userToUpdate.setEmail(updates.email());
        }

        if (updates.username() != null) {
            String newUsername = updates.username();

            if (!newUsername.equals(userToUpdate.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
                throw new IllegalArgumentException("Username already in use");
            }
            userToUpdate.setUsername(newUsername);
            System.out.println(newUsername);
        }

        if (updates.password() != null) {

            userToUpdate.setPassword(passwordEncoder.encode(updates.password()));
        }

        if (updates.bio() != null) {
            userToUpdate.setBio(updates.bio().isEmpty()?null:updates.bio());
        }
        if (updates.image()!= null && updates.image().contains("/uploads/temp/")) {
            // 头像是一个临时URL，需要转正
            Map<String, String> urlMapping = fileStorageService.promoteFiles(List.of(updates.image()));
            String finalImageUrl = urlMapping.get(updates.image());
            userToUpdate.setImage(finalImageUrl);
        } else if (updates.image()!= null) {
            // 用户可能提供了一个非我们系统的URL，或者一个已经转正的URL
            userToUpdate.setImage(updates.image());
        }

        User savedUser = userRepository.save(userToUpdate);
        String token = jwtService.generateToken(savedUser); // 更新后颁发一个新 token
        return buildUserResponse(savedUser, token);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new  ResourceNotFoundException("User not found for id: " + id));
    }

    // 简单的向量加权平均
    public String updateUserEmbedding(String oldEmbeddingStr, float[] newArticleVector) {
        float[] oldVector = parseEmbedding(oldEmbeddingStr); // 字符串转float[]
        if (oldVector == null) {
            // 如果用户是新的，直接用当前文章向量作为初始兴趣
            return serializeEmbedding(newArticleVector);
        }

        // 移动平均: 0.8 旧兴趣 + 0.2 新兴趣
        float alpha = 0.2f;
        double sumSq = 0.0;

        // 1. 更新
        for (int i = 0; i < oldVector.length; i++) {
            oldVector[i] = oldVector[i] * (1 - alpha) + newArticleVector[i] * alpha;
            sumSq += oldVector[i] * oldVector[i];
        }

        // 2. 归一化 (L2 Normalize)
        // 这一步保证向量模长始终为1，对于 Cosine 距离检索非常重要
        float norm = (float) Math.sqrt(sumSq);
        if (norm > 1e-6) { // 防止除以0
            for (int i = 0; i < oldVector.length; i++) {
                oldVector[i] /= norm;
            }
        }

        return serializeEmbedding(oldVector);
    }

    // 工具: String -> float[]
    private float[] parseEmbedding(String str) {
        if (str == null || str.isBlank()) return null;
        String[] parts = str.split(",");
        float[] res = new float[parts.length];
        for(int i=0; i<parts.length; i++) res[i] = Float.parseFloat(parts[i]);
        return res;
    }

    // 工具: float[] -> String
    private String serializeEmbedding(float[] vec) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        return sb.toString();
    }
}