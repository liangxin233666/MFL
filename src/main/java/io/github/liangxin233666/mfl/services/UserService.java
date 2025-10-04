package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.LoginUserRequest;
import io.github.liangxin233666.mfl.dtos.NewUserRequest;
import io.github.liangxin233666.mfl.dtos.UserResponse;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.liangxin233666.mfl.dtos.UpdateUserRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 使用构造器注入所有依赖
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public UserResponse getCurrentUser(org.springframework.security.core.userdetails.User currentUser) {

        Long userId = Long.valueOf(currentUser.getUsername());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return buildUserResponse(user, token);
    }

    @Transactional
    public UserResponse updateCurrentUser(@Valid UpdateUserRequest request, UserDetails currentUserDetails) {
        Long currentUserId = Long.valueOf(currentUserDetails.getUsername());
        User userToUpdate = findUserById(currentUserId);

        UpdateUserRequest.UserDto updates = request.user();


        if (updates.email() != null) {

            if (!updates.email().equals(userToUpdate.getEmail()) && userRepository.findByEmail(updates.email()).isPresent()) {
                throw new IllegalArgumentException("Email already in use");
            }
            userToUpdate.setEmail(updates.email());
        }

        // 2. 处理 Username 更新 - 这就是您问题的核心
        if (updates.username() != null) {
            String newUsername = updates.username();


            if (!newUsername.equals(userToUpdate.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
                throw new IllegalArgumentException("Username already in use");
            }
            userToUpdate.setUsername(newUsername);
        }


        if (updates.password() != null) {

            userToUpdate.setPassword(passwordEncoder.encode(updates.password()));
        }


        if (updates.bio() != null) {
            userToUpdate.setBio(updates.bio());
        }
        if (updates.image() != null) {
            userToUpdate.setImage(updates.image());
        }

        User savedUser = userRepository.save(userToUpdate);
        String token = jwtService.generateToken(savedUser); // 更新后可以颁发一个新 token
        return buildUserResponse(savedUser, token);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new  ResourceNotFoundException("User not found for id: " + id));
    }
}