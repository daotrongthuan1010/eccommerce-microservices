package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import com.dtthuan3.ecommerce.productservice.domain.ProductRevision;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductDraftRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.mapper.ProductMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeOptionRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateItemRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRevisionRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductRevisionRepository productRevisionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    private final AttributeTemplateRepository attributeTemplateRepository;
    private final AttributeTemplateItemRepository attributeTemplateItemRepository;

    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createDraft(CreateProductDraftRequest request) {

        // 1. Kiểm tra mã nghiệp vụ.
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Mã sản phẩm đã tồn tại: " + request.getProductCode()
            );
        }

        // 2. Kiểm tra template và các thuộc tính đầu vào.
        List<AttributeTemplateItem> templateItems =
                validateTemplate(request.getAttributeTemplateId());

        List<ProductAttributeValueRequest> attributeRequests =
                request.getAttributes() == null
                        ? List.of()
                        : request.getAttributes();

        validateDuplicateAttributes(attributeRequests);

        Map<UUID, Attribute> attributes =
                loadAttributes(attributeRequests);

        validateAttributesAgainstTemplate(
                attributeRequests,
                attributes,
                templateItems
        );

        Map<UUID, AttributeOption> options =
                loadOptions(attributeRequests);

        validateAttributeValues(attributeRequests, attributes, options);

        // 3. Tạo SPU.
        Product product = new Product();
        product.setProductCode(request.getProductCode().trim());
        product.setCategoryId(request.getCategoryId());
        product.setBrandId(request.getBrandId());
        product.setManufacturerId(request.getManufacturerId());
        product.setAttributeTemplateId(request.getAttributeTemplateId());
        product.setStatus(ProductStatus.DRAFT);

        Product savedProduct = productRepository.save(product);

        // 4. Tạo revision đầu tiên.
        ProductRevision revision = new ProductRevision();
        revision.setProductId(savedProduct.getId());
        revision.setVersion(1L);
        revision.setName(request.getName().trim());
        revision.setShortDescription(request.getShortDescription());
        revision.setDescription(request.getDescription());
        revision.setOriginCountry(request.getOriginCountry());
        revision.setStorageCondition(request.getStorageCondition());
        revision.setDeliveryCondition(request.getDeliveryCondition());
        revision.setReturnPolicy(request.getReturnPolicy());

        ProductRevision savedRevision =
                productRevisionRepository.save(revision);

        // 5. Lưu các giá trị thuộc tính vào revision.
        List<ProductAttributeValue> values = new ArrayList<>();

        for (ProductAttributeValueRequest attributeRequest : attributeRequests) {
            ProductAttributeValue value = new ProductAttributeValue();

            value.setRevisionId(savedRevision.getId());
            value.setAttributeId(attributeRequest.getAttributeId());
            value.setOptionId(attributeRequest.getOptionId());
            value.setValueText(attributeRequest.getValueText());
            value.setValueNumber(attributeRequest.getValueNumber());
            value.setValueDate(attributeRequest.getValueDate());
            value.setValueBoolean(attributeRequest.getValueBoolean());

            values.add(value);
        }

        if (!values.isEmpty()) {
            productAttributeValueRepository.saveAll(values);
        }

        // 6. Gắn revision hiện tại vào Product.
        savedProduct.setCurrentRevisionId(savedRevision.getId());
        productRepository.save(savedProduct);

        // 7. Trả kết quả.
        return productMapper.toResponse(savedProduct, savedRevision);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", productId));

        ProductRevision revision = null;

        if (product.getCurrentRevisionId() != null) {
            revision = productRevisionRepository
                    .findById(product.getCurrentRevisionId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "ProductRevision",
                                    product.getCurrentRevisionId()
                            ));
        }

        return productMapper.toResponse(product, revision);
    }

    private List<AttributeTemplateItem> validateTemplate(UUID templateId) {

        if (templateId == null) {
            return List.of();
        }

        attributeTemplateRepository.findById(templateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AttributeTemplate",
                                templateId
                        ));

        return attributeTemplateItemRepository
                .findAllByTemplateIdAndActiveTrueOrderByDisplayOrderAsc(templateId);
    }

    private void validateDuplicateAttributes(
            List<ProductAttributeValueRequest> requests
    ) {
        Set<UUID> ids = new HashSet<>();

        for (ProductAttributeValueRequest request : requests) {
            if (!ids.add(request.getAttributeId())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Thuộc tính bị khai báo nhiều lần: "
                                + request.getAttributeId()
                );
            }
        }
    }

    private Map<UUID, Attribute> loadAttributes(
            List<ProductAttributeValueRequest> requests
    ) {
        Set<UUID> ids = requests.stream()
                .map(ProductAttributeValueRequest::getAttributeId)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Attribute> result =
                attributeRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(
                                Attribute::getId,
                                Function.identity()
                        ));

        if (result.size() != ids.size()) {
            Set<UUID> missing = new HashSet<>(ids);
            missing.removeAll(result.keySet());

            throw new ResourceNotFoundException(
                    "Không tìm thấy thuộc tính: " + missing
            );
        }

        return result;
    }

    private void validateAttributesAgainstTemplate(
            List<ProductAttributeValueRequest> requests,
            Map<UUID, Attribute> attributes,
            List<AttributeTemplateItem> templateItems
    ) {
        if (templateItems.isEmpty()) {
            return;
        }

        Map<UUID, AttributeTemplateItem> templateAttributes =
                templateItems.stream().collect(Collectors.toMap(
                        AttributeTemplateItem::getAttributeId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        for (ProductAttributeValueRequest request : requests) {
            if (!templateAttributes.containsKey(request.getAttributeId())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Thuộc tính không thuộc template đã chọn: "
                                + request.getAttributeId()
                );
            }
        }

        Set<UUID> suppliedIds = requests.stream()
                .map(ProductAttributeValueRequest::getAttributeId)
                .collect(Collectors.toSet());

        for (AttributeTemplateItem item : templateItems) {
            // Giả định entity có trường boolean required.
            if (Boolean.TRUE.equals(item.getRequired())
                    && !suppliedIds.contains(item.getAttributeId()))  {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Thiếu thuộc tính bắt buộc: " + item.getAttributeId()
                );
            }
        }
    }

    private Map<UUID, AttributeOption> loadOptions(
            List<ProductAttributeValueRequest> requests
    ) {
        Set<UUID> optionIds = requests.stream()
                .map(ProductAttributeValueRequest::getOptionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (optionIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AttributeOption> result =
                attributeOptionRepository.findAllById(optionIds).stream()
                        .collect(Collectors.toMap(
                                AttributeOption::getId,
                                Function.identity()
                        ));

        if (result.size() != optionIds.size()) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy một hoặc nhiều lựa chọn thuộc tính"
            );
        }

        return result;
    }

    private void validateAttributeValues(
            List<ProductAttributeValueRequest> requests,
            Map<UUID, Attribute> attributes,
            Map<UUID, AttributeOption> options
    ) {
        for (ProductAttributeValueRequest request : requests) {
            Attribute attribute = attributes.get(request.getAttributeId());

            if (attribute == null) {
                continue;
            }

            // Kiểm tra option phải thuộc đúng attribute.
            if (request.getOptionId() != null) {
                AttributeOption option = options.get(request.getOptionId());

                if (option == null) {
                    throw new ResourceNotFoundException(
                            "AttributeOption",
                            request.getOptionId()
                    );
                }

                // Giả định AttributeOption có trường attributeId.
                if (!attribute.getId().equals(option.getAttributeId())) {
                    throw new BusinessException(
                            ErrorCode.VALIDATION_ERROR,
                            "Lựa chọn không thuộc thuộc tính "
                                    + attribute.getId()
                    );
                }
            }

            /*
             * Cần bổ sung switch theo AttributeDataType thực tế
             * của entity Attribute để bảo đảm TEXT, NUMBER, DATE,
             * BOOLEAN và SELECT chỉ nhận đúng loại giá trị.
             */
        }
    }
}