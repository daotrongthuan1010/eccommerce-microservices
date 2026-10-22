package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import com.dtthuan3.ecommerce.authservice.iam.entity.Tenant;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroup;
import com.dtthuan3.ecommerce.authservice.iam.enums.UserStatus;
import com.dtthuan3.ecommerce.authservice.iam.repository.AuthUserRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.TenantRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JIT provisioning (P2): tao dong auth_users khi thay Keycloak sub la lan dau.
 *
 * <p>Idempotent: kiem tra theo id (sub) truoc khi tao; neu 2 request dong thoi bi dua
 * unique constraint thi doc lai ban ghi da ton tai thay vi bao loi. Username/email trung
 * (cua sub khac) thi fallback an toan thay vi vi pham unique constraint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IamProvisioningService {

    private final AuthUserRepository authUserRepository;
    private final TenantRepository tenantRepository;
    private final UserGroupRepository userGroupRepository;
    private final AuthContextService authContextService;
    private final IamProvisioningProperties properties;

    @Transactional
    public AuthUser provisionIfAbsent(String subject, String username, String fullName, String email) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject (Keycloak sub) must not be blank");
        }
        return authUserRepository.findById(subject).orElseGet(() -> create(subject, username, fullName, email));
    }

    private AuthUser create(String subject, String username, String fullName, String email) {
        try {
            AuthUser.AuthUserBuilder builder = AuthUser.builder()
                    .id(subject)
                    .username(resolveUsername(subject, username))
                    .fullName(fullName)
                    .email(resolveEmail(subject, email))
                    .status(UserStatus.ACTIVE)
                    .isSystemAdmin(false);

            Tenant tenant = resolveDefaultTenant();
            if (tenant != null) {
                builder.tenant(tenant);
            }
            UserGroup group = resolveDefaultGroup();
            if (group != null) {
                builder.group(group);
            }

            AuthUser saved = authUserRepository.saveAndFlush(builder.build());
            authContextService.invalidate(subject);
            log.info(
                    "JIT provisioned auth_user id={} username={} tenant={}",
                    subject,
                    saved.getUsername(),
                    saved.getTenant() != null ? saved.getTenant().getCode() : "-");
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Dua request dong thoi: ban ghi da duoc tao boi thread khac -> doc lai.
            log.warn("JIT race on subject {}, reloading existing record", subject);
            return authUserRepository.findById(subject).orElseThrow(() -> e);
        }
    }

    private String resolveUsername(String subject, String username) {
        String candidate = username != null && !username.isBlank() ? username : "kc-" + shortId(subject);
        if (authUserRepository.findByUsername(candidate).isEmpty()) {
            return candidate;
        }
        // Username da thuoc ve sub khac -> suffix de tranh unique violation.
        String fallback = candidate + "-" + shortId(subject);
        log.warn("Username {} already taken, using fallback {}", candidate, fallback);
        return fallback;
    }

    private String resolveEmail(String subject, String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        boolean taken = authUserRepository
                .findByEmail(email)
                .map(u -> !subject.equals(u.getId()))
                .orElse(false);
        if (taken) {
            log.warn("Email {} already taken by another subject, storing null for {}", email, subject);
            return null;
        }
        return email;
    }

    private Tenant resolveDefaultTenant() {
        String code = properties.getDefaultTenantCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        return tenantRepository.findByCode(code).orElseGet(() -> {
            log.warn("Default tenant code {} not found in seed, provisioning without tenant", code);
            return null;
        });
    }

    private UserGroup resolveDefaultGroup() {
        Long groupId = properties.getDefaultGroupId();
        if (groupId == null) {
            return null;
        }
        return userGroupRepository.findById(groupId).orElseGet(() -> {
            log.warn("Default group id {} not found in seed, provisioning without group", groupId);
            return null;
        });
    }

    private static String shortId(String subject) {
        return subject.length() <= 8 ? subject : subject.substring(0, 8);
    }
}
