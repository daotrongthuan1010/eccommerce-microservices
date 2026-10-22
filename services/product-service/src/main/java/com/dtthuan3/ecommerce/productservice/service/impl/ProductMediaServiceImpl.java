
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.grpc.file.UploadFileResponse;
import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import com.dtthuan3.ecommerce.productservice.grpc.client.FileGrpcClient;
import com.dtthuan3.ecommerce.productservice.mapper.ProductMediaMapper;
import com.dtthuan3.ecommerce.productservice.mapper.SkuMediaMapper;
import com.dtthuan3.ecommerce.productservice.repository.ProductMediaRepository;
import com.dtthuan3.ecommerce.productservice.repository.SkuMediaRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductMediaServiceImpl implements ProductMediaService {

    private final ProductMediaRepository productMediaRepository;
    private final SkuMediaRepository skuMediaRepository;
    private final ProductMediaMapper productMediaMapper;
    private final SkuMediaMapper skuMediaMapper;
    private final FileGrpcClient fileGrpcClient;

    @Override
    @Transactional
    public ProductMediaResponse uploadProductMedia(
            UUID productId,
            UUID revisionId,
            MultipartFile file,
            Integer displayOrder,
            Boolean primary
    ) {
        validateFile(file);

        UploadFileResponse uploaded =
                fileGrpcClient.upload(file, "products/" + productId);

        String fileKey = uploaded.getKey();
        validateFileKey(fileKey);

        ProductMedia media = ProductMedia.builder()
                .productId(productId)
                .revisionId(revisionId)
                .fileKey(fileKey)
                .mediaType(resolveMediaType(file))
                .displayOrder(validateDisplayOrder(displayOrder))
                .primary(Boolean.TRUE.equals(primary))
                .build();

        return productMediaMapper.toResponse(
                productMediaRepository.save(media)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductMediaResponse getProductMediaById(UUID id) {
        ProductMedia media = productMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy product media với id: " + id
                ));

        return productMediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    public ProductMediaResponse updateProductMedia(
            UUID id,
            Integer displayOrder,
            Boolean primary
    ) {
        ProductMedia media = productMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy product media với id: " + id
                ));

        if (displayOrder != null) {
            media.setDisplayOrder(validateDisplayOrder(displayOrder));
        }

        if (primary != null) {
            media.setPrimary(primary);
        }

        return productMediaMapper.toResponse(
                productMediaRepository.save(media)
        );
    }

    @Override
    @Transactional
    public void deleteProductMedia(UUID id) {
        ProductMedia media = productMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy product media với id: " + id
                ));

        productMediaRepository.delete(media);
    }

    @Override
    @Transactional
    public SkuMediaResponse uploadSkuMedia(
            UUID skuId,
            UUID revisionId,
            MultipartFile file,
            Integer displayOrder,
            Boolean primary
    ) {
        validateFile(file);

        UploadFileResponse uploaded =
                fileGrpcClient.upload(file, "skus/" + skuId);

        String fileKey = uploaded.getKey();
        validateFileKey(fileKey);

        SkuMedia media = SkuMedia.builder()
                .skuId(skuId)
                .revisionId(revisionId)
                .fileKey(fileKey)
                .mediaType(resolveMediaType(file))
                .displayOrder(validateDisplayOrder(displayOrder))
                .primary(Boolean.TRUE.equals(primary))
                .build();

        return skuMediaMapper.toResponse(
                skuMediaRepository.save(media)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SkuMediaResponse getSkuMediaById(UUID id) {
        SkuMedia media = skuMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy SKU media với id: " + id
                ));

        return skuMediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    public SkuMediaResponse updateSkuMedia(
            UUID id,
            Integer displayOrder,
            Boolean primary
    ) {
        SkuMedia media = skuMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy SKU media với id: " + id
                ));

        if (displayOrder != null) {
            media.setDisplayOrder(validateDisplayOrder(displayOrder));
        }

        if (primary != null) {
            media.setPrimary(primary);
        }

        return skuMediaMapper.toResponse(
                skuMediaRepository.save(media)
        );
    }

    @Override
    @Transactional
    public void deleteSkuMedia(UUID id) {
        SkuMedia media = skuMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy SKU media với id: " + id
                ));

        skuMediaRepository.delete(media);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống");
        }
    }

    private void validateFileKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalStateException(
                    "File-service không trả về key hợp lệ"
            );
        }
    }

    private Integer validateDisplayOrder(Integer displayOrder) {
        int value = displayOrder == null ? 0 : displayOrder;

        if (value < 0) {
            throw new IllegalArgumentException(
                    "Thứ tự hiển thị phải lớn hơn hoặc bằng 0"
            );
        }

        return value;
    }

    private MediaType resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType != null && contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }

        return MediaType.OTHER;
    }
}
