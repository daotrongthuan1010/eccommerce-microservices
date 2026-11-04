package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryWarehouseAccessGrantRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryWarehouseAccessGrantRepository.WarehouseAccessGrantRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Quản lý ACL theo kho và kiểm tra phạm vi truy cập của principal hiện tại.
 * ADMIN quản trị toàn bộ kho; principal khác phải có grant theo warehouse. Tác vụ nền không có Authentication
 * được phép chạy theo quyền hệ thống của service, còn mọi API HTTP vẫn bị chặn bởi cấu hình Spring Security.
 */
@Service
@RequiredArgsConstructor
public class InventoryWarehouseAuthorizationService {
    private final InventoryWarehouseAccessGrantRepository grantRepository;

    /** Bảo đảm principal đã đăng nhập chỉ thao tác trên kho đã được cấp phạm vi. */
    @Transactional(readOnly = true)
    public void requireWarehouseAccess(String warehouseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return;
        requireWarehouseAccess(warehouseId, authentication);
    }

    /** Kiểm tra warehouse grant cho adapter như gRPC, nơi principal được giữ trong RPC context riêng. */
    @Transactional(readOnly = true)
    public void requireWarehouseAccess(String warehouseId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Yêu cầu principal đã xác thực để kiểm tra phạm vi Warehouse");
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) return;
        if (!grantRepository.hasAccess(warehouseId, authentication.getName())) {
            throw new AccessDeniedException("Principal chưa được cấp quyền truy cập Warehouse " + warehouseId);
        }
    }

    /** Trả kết quả scope-check cho màn hình liệt kê kho đã được cấp quyền. */
    @Transactional(readOnly = true)
    public boolean hasWarehouseAccess(String warehouseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))
                || grantRepository.hasAccess(warehouseId, authentication.getName());
    }

    /** Cho phép lịch sử audit không lọc theo inventory chỉ với quyền đọc audit toàn kho. */
    public void requireUnfilteredAuditAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Yêu cầu principal đã xác thực để đọc audit toàn kho");
        }
        boolean administrator = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean auditReader = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_INVENTORY_AUDIT_READ"));
        boolean globalReader = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_INVENTORY_READ_ALL"));
        if (!administrator && !(auditReader && globalReader)) {
            throw new AccessDeniedException("Cần INVENTORY_AUDIT_READ và INVENTORY_READ_ALL để đọc audit toàn kho");
        }
    }

    /** Quản trị viên cấp quyền warehouse scope cho subject JWT ổn định. */
    @Transactional
    public WarehouseAccessGrantRecord grant(String warehouseId, String subjectId, String grantedBy) {
        String subject = normalize(subjectId, "subjectId");
        String actor = normalize(grantedBy, "grantedBy");
        Instant now = Instant.now();
        if (!grantRepository.grant(warehouseId, subject, actor, now)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Principal đã được cấp phạm vi truy cập kho này");
        }
        return new WarehouseAccessGrantRecord(warehouseId, subject, actor, now);
    }

    /** Thu hồi đúng một grant; các quyền trên kho khác không bị ảnh hưởng. */
    @Transactional
    public void revoke(String warehouseId, String subjectId) {
        if (!grantRepository.revoke(warehouseId, normalize(subjectId, "subjectId"))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy grant của subject trên warehouse");
        }
    }

    /** Liệt kê grant để quản trị kiểm tra cấu hình quyền theo kho. */
    @Transactional(readOnly = true)
    public List<WarehouseAccessGrantRecord> list(String warehouseId) {
        return grantRepository.listByWarehouse(warehouseId);
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + " không được để trống");
        }
        return value.trim();
    }
}
