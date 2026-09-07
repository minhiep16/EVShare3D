package com.example.evshare.service.impl;

import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.entity.User;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(Long userId, boolean includeRoles) {
        if (userId == null) {
            throw new BusinessException("User ID must not be null", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User profile lookup failed: User with ID [{}] not found", userId);
                    return new BusinessException("User profile not found", HttpStatus.NOT_FOUND);
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("User profile lookup blocked: User [{}] is deactivated", userId);
            throw new BusinessException("User account is disabled or deactivated", HttpStatus.FORBIDDEN);
        }

        UserResponse response = UserResponse.fromEntity(user);
        if (!includeRoles) {
            response.setRoles(null);
        }

        log.debug("Successfully retrieved profile for user ID [{}]", userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException("User ID must not be null", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("User not found with ID: " + targetUserId, HttpStatus.NOT_FOUND));
        return UserResponse.fromEntity(user);
    }
}
