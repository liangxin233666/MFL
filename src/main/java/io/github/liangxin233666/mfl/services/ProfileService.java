package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.ProfileResponse;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "profiles", key = "#username")
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username, UserDetails currentUserDetails) {
        User userToFind = findUserByUsername(username);

        // 如果 currentUserDetails 是 null，意味着是匿名访问
        if (currentUserDetails == null) {
            return buildProfileResponse(userToFind, false);
        }

        // 如果是登录用户访问，需要判断是否已关注
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));
        boolean isFollowing = currentUser.getFollowing().contains(userToFind);
        return buildProfileResponse(userToFind, isFollowing);
    }

    @Transactional
    public ProfileResponse followUser(String username, UserDetails currentUserDetails) {
        User userToFollow = findUserByUsername(username);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        currentUser.getFollowing().add(userToFollow);
        userRepository.save(currentUser);

        return buildProfileResponse(userToFollow, true);
    }

    @Transactional
    public ProfileResponse unfollowUser(String username, UserDetails currentUserDetails) {
        User userToUnfollow = findUserByUsername(username);
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        currentUser.getFollowing().remove(userToUnfollow);
        userRepository.save(currentUser);

        return buildProfileResponse(userToUnfollow, false);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for username: " + username));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("User not found for id: " + id));
    }

    private ProfileResponse buildProfileResponse(User user, boolean following) {
        ProfileResponse.ProfileDto profileDto = new ProfileResponse.ProfileDto(
                user.getUsername(),
                user.getBio(),
                user.getImage(),
                following
        );
        return new ProfileResponse(profileDto);
    }
}