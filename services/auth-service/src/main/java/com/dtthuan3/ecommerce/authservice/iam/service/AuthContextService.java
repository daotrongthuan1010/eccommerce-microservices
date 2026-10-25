package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.dto.AuthContext;
import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole;
import com.dtthuan3.ecommerce.authservice.iam.enums.UserStatus;
import com.dtthuan3.ecommerce.authservice.iam.repository.AuthUserRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRoleRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthContextService - gop quyen truc tiep (UserRole) + thua ke tu group (UserGroupRole), cache Redis 30
 * phut.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthContextService {

    private static final String CACHE_PREFIX = "iam:ctx:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final AuthUserRepository authUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserGroupRoleRepository groupRoleRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AuthContext getContext(String userId) {
        AuthContext cached = fromCache(userId);
        if (cached != null) {
            return cached;
        }
        AuthContext ctx = buildFromDb(userId);
        if (ctx != null) {
            toCache(userId, ctx);
        }
        return ctx;
    }

    @Transactional(readOnly = true)
    public AuthContext refreshContext(String userId) {
        invalidate(userId);
        return getContext(userId);
    }

    public void invalidate(String userId) {
        redis.delete(CACHE_PREFIX + userId);
    }

    public void invalidateAll(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> keys = userIds.stream().map(id -> CACHE_PREFIX + id).collect(Collectors.toList());
        redis.delete(keys);
    }

    public boolean isCached(String userId) {
        return Boolean.TRUE.equals(redis.hasKey(CACHE_PREFIX + userId));
    }

    @Transactional(readOnly = true)
    public AuthContext buildFromDb(String userId) {
        Optional<AuthUser> opt = authUserRepository.findByIdWithContext(userId);
        if (opt.isEmpty()) {
            return null;
        }
        AuthUser user = opt.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("User {} is not ACTIVE (status={})", userId, user.getStatus());
            return null;
        }

        List<UserRole> directRoles = userRoleRepository.findByUserIdWithRolePermissions(userId);
        List<UserGroupRole> groupRoles = user.getGroup() != null
                ? groupRoleRepository.findByGroupIdWithRolePermissions(user.getGroup().getId())
                : Collections.emptyList();

        Set<String> roleCodes = new HashSet<>();
        directRoles.forEach(ur -> roleCodes.add(ur.getRole().getCode()));
        groupRoles.forEach(ugr -> roleCodes.add(ugr.getRole().getCode()));

        Set<String> permissions = new HashSet<>();
        directRoles.forEach(ur -> ur.getRole().getRolePermissions()
                .forEach(rp -> permissions.add(rp.getPermission().getCode())));
        groupRoles.forEach(ugr -> ugr.getRole().getRolePermissions()
                .forEach(rp -> permissions.add(rp.getPermission().getCode())));

        AuthContext.AuthContextBuilder builder = AuthContext.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .isSystemAdmin(user.getIsSystemAdmin())
                .roleCodes(roleCodes)
                .permissions(permissions);
        if (user.getTenant() != null) {
            builder.tenantId(user.getTenant().getId())
                    .tenantCode(user.getTenant().getCode())
                    .tenantName(user.getTenant().getName());
        }
        if (user.getGroup() != null) {
            builder.groupId(user.getGroup().getId()).groupName(user.getGroup().getName());
            if (user.getGroup().getGroupType() != null) {
                builder.groupTypeCode(user.getGroup().getGroupType().getCode())
                        .groupTypeName(user.getGroup().getGroupType().getName());
            }
            if (user.getGroup().getParent() != null) {
                builder.parentGroupId(user.getGroup().getParent().getId());
            }
        }
        if (user.getPosition() != null) {
            builder.positionId(user.getPosition().getId())
                    .positionName(user.getPosition().getName())
                    .positionLevel(user.getPosition().getLevel());
        }
        return builder.build();
    }

    private AuthContext fromCache(String userId) {
        try {
            String json = redis.opsForValue().get(CACHE_PREFIX + userId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, AuthContext.class);
        } catch (Exception e) {
            log.warn("Read auth context cache failed userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private void toCache(String userId, AuthContext ctx) {
        try {
            redis.opsForValue()
                    .set(CACHE_PREFIX + userId, objectMapper.writeValueAsString(ctx), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Write auth context cache failed userId={}: {}", userId, e.getMessage());
        }
    }
}
