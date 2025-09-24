package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.LoginUserRequest;
import io.github.liangxin233666.mfl.dtos.NewUserRequest;
import io.github.liangxin233666.mfl.dtos.UserResponse;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    public UserResponse registerNewUser(NewUserRequest request) {
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

    public UserResponse loginUser(LoginUserRequest request) {
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
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return buildUserResponse(user, token);
    }
}