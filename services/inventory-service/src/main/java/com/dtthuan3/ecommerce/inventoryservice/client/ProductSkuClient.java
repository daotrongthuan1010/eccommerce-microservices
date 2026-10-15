package com.dtthuan3.ecommerce.inventoryservice.client;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import com.dtthuan3.ecommerce.inventoryservice.repository.SkuCatalogProjectionRepository;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveSkuValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;

/** Đối soát trạng thái SKU với projection Kafka hoặc gọi Product Service khi projection chưa đủ mới. */
@Component
/** Adapter tích hợp Product Service và projection cục bộ cho cổng xác thực SKU. */
public class ProductSkuClient implements ActiveSkuValidator {

    public record SkuValidationRequest(List<String> skuCodes) {}
    public record SkuValidationResponse(List<String> activeSkuCodes, List<String> invalidSkuCodes) {}

    private final RestClient restClient;
    private final SkuCatalogProjectionRepository projectionRepository;

    @Value("${inventory.sku-projection.max-age:PT5M}")
    private Duration projectionMaxAge;

    public ProductSkuClient(RestClient.Builder productServiceRestClientBuilder,
                            SkuCatalogProjectionRepository projectionRepository) {
        this.restClient = productServiceRestClientBuilder.baseUrl("http://product-service").build();
        this.projectionRepository = projectionRepository;
    }

    /**
     * Kiểm tra các mã SKU trước khi mở transaction ghi Inventory. Nếu được gọi khi đang có transaction,
     * tạm dừng transaction đó trong lúc gọi qua mạng để không giữ khóa dòng Inventory khi gọi service khác.
     */
    /**
     * Xác thực theo lô. Projection còn mới được dùng tại chỗ; SKU thiếu/cũ mới gọi Product Service.
     * Không tham gia transaction của caller để tránh giữ khóa và connection ghi DB trong lúc gọi mạng.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void requireActiveSkus(Collection<String> skuCodes) {
        // Chuẩn hóa danh sách để tránh gọi lặp cùng SKU; input rỗng là lỗi request thay vì coi như hợp lệ.
        List<String> codes = skuCodes == null ? List.of() : skuCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Phải cung cấp ít nhất một SKU");
        }

        Instant now = Instant.now();
        Instant freshAfter = now.minus(projectionMaxAge);
        // Đọc projection theo lô, tránh query một lần cho từng SKU trong request batch.
        Map<String, SkuCatalogProjection> projected = projectionRepository.findAllBySkuCodeIn(codes).stream()
                .collect(Collectors.toMap(SkuCatalogProjection::getSkuCode, sku -> sku));
        List<String> toReconcile = codes.stream()
                .filter(code -> {
                    SkuCatalogProjection sku = projected.get(code);
                    return sku == null || sku.getSourceUpdatedAt() == null
                            || sku.getSourceUpdatedAt().isBefore(freshAfter);
                })
                .toList();

        // Projection còn hạn dùng trực tiếp; chỉ các SKU thiếu/cũ mới phải gọi Product Service.
        Map<String, Boolean> activeByCode = projected.entrySet().stream()
                .filter(entry -> !toReconcile.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().isActive()));

        if (!toReconcile.isEmpty()) {
            SkuValidationResponse result = validateWithProduct(toReconcile);
            Set<String> active = new HashSet<>(result.activeSkuCodes());
            Set<String> invalid = new HashSet<>(result.invalidSkuCodes());
            activeByCode.putAll(active.stream().collect(Collectors.toMap(code -> code, code -> true)));
            activeByCode.putAll(invalid.stream().collect(Collectors.toMap(code -> code, code -> false)));

            // Lưu kết quả đối soát để request sau có thể dùng local projection thay vì gọi mạng tiếp.
            projectionRepository.saveAll(codesForProjection(toReconcile, active, now));
        }

        List<String> invalidSkus = codes.stream()
                .filter(code -> !Boolean.TRUE.equals(activeByCode.get(code)))
                .toList();
        if (!invalidSkus.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "SKU không tồn tại hoặc đã ngừng hoạt động: " + String.join(", ", invalidSkus));
        }
    }

    private SkuValidationResponse validateWithProduct(List<String> codes) {
        SkuValidationResponse result;
        try {
            result = restClient.post()
                    .uri("/skus/validate")
                    .headers(headers -> {
                        // Chuyển tiếp JWT hiện tại để Product Service áp dụng cùng danh tính/quyền người gọi.
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication instanceof JwtAuthenticationToken jwt) {
                            headers.setBearerAuth(jwt.getToken().getTokenValue());
                        }
                    })
                    .body(new SkuValidationRequest(codes))
                    .retrieve()
                    .body(SkuValidationResponse.class);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.DEPENDENCY_ERROR,
                    "Không thể xác thực SKU với product-service; inventory chưa được ghi");
        }

        if (result == null || result.activeSkuCodes() == null || result.invalidSkuCodes() == null) {
            throw new BusinessException(ErrorCode.DEPENDENCY_ERROR,
                    "product-service trả về kết quả xác thực SKU rỗng");
        }
        Set<String> active = new HashSet<>(result.activeSkuCodes());
        Set<String> invalid = new HashSet<>(result.invalidSkuCodes());
        Set<String> classified = new HashSet<>(active);
        classified.addAll(invalid);
        // Nếu Product Service bỏ sót hoặc phân loại một SKU vào cả hai danh sách, không được ghi tồn.
        if (!classified.equals(new HashSet<>(codes)) || active.stream().anyMatch(invalid::contains)) {
            throw new BusinessException(ErrorCode.DEPENDENCY_ERROR,
                    "product-service trả về kết quả xác thực SKU không nhất quán");
        }
        return result;
    }

    private List<SkuCatalogProjection> codesForProjection(List<String> codes, Set<String> active, Instant updatedAt) {
        return codes.stream()
                .map(code -> new SkuCatalogProjection(code, active.contains(code), null, updatedAt))
                .toList();
    }
}
