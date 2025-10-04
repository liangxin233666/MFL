package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws ResourceNotFoundException {
        // 我们用 userRepository 来根据ID查找用户
        return userRepository.findById(Long.valueOf(userId))
                .map(user -> new User(
                        user.getId().toString(),
                        user.getPassword(),
                        Collections.emptyList()
                ))
                .orElseThrow(() -> new  ResourceNotFoundException("User not found with id: " + userId));
    }
}