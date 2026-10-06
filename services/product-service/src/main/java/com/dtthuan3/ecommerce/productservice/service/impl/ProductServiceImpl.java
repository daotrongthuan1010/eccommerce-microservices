package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductAttributeValueResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import com.dtthuan3.ecommerce.productservice.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.productservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.ProductAttributeValueMapper;
import com.dtthuan3.ecommerce.productservice.mapper.ProductMapper;
import com.dtthuan3.ecommerce.productservice.repository.ProductAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductPublishingHistoryRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor

public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductPublishingHistoryRepository productPublishingHistoryRepository;

    private final ProductMapper productMapper;
    private final ProductAttributeValueMapper productAttributeValueMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {

        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException("Product code đã tồn tại: " + request.getProductCode());
        }

        Product product = productMapper.toEntity(request);

        product.setStatus(ProductStatus.DRAFT);

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        if (productRepository.existsByProductCodeAndIdNot(request.getProductCode(), id)) {
            throw new DuplicateResourceException("Product code đã tồn tại: " + request.getProductCode());
        }

        productMapper.updateEntity(request, product);

        Product updatedProduct = productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        productRepository.delete(product);
    }

    // =========================
    // Product Attribute Value
    // =========================

    @Override
    @Transactional
    public ProductAttributeValueResponse createAttributeValue(ProductAttributeValueRequest request) {

        if (!productRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException("Không tìm thấy product với id: " + request.getProductId());
        }

        if (productAttributeValueRepository.existsByProductIdAndAttributeId(request.getProductId(), request.getAttributeId())) {

            throw new DuplicateResourceException("Attribute đã tồn tại trong product");
        }

        ProductAttributeValue entity = productAttributeValueMapper.toEntity(request);

        ProductAttributeValue saved = productAttributeValueRepository.save(entity);

        return productAttributeValueMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueResponse getAttributeValueById(Long id) {

        ProductAttributeValue entity = productAttributeValueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product attribute value với id: " + id));

        return productAttributeValueMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ProductAttributeValueResponse updateAttributeValue(Long id, ProductAttributeValueRequest request) {

        ProductAttributeValue entity = productAttributeValueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product attribute value với id: " + id));

        if (!productRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException("Không tìm thấy product với id: " + request.getProductId());
        }

        if (productAttributeValueRepository.existsByProductIdAndAttributeIdAndIdNot(request.getProductId(), request.getAttributeId(), id)) {

            throw new DuplicateResourceException("Attribute đã tồn tại trong product");
        }

        productAttributeValueMapper.updateEntity(request, entity);

        ProductAttributeValue updated = productAttributeValueRepository.save(entity);

        return productAttributeValueMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAttributeValue(Long id) {

        ProductAttributeValue entity = productAttributeValueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product attribute value với id: " + id));

        productAttributeValueRepository.delete(entity);
    }

    // =========================
    // Publish / Unpublish
    // =========================

    @Override
    @Transactional
    public ProductResponse submitForApproval(Long id) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        ProductStatus oldStatus = product.getStatus();

        if (oldStatus != ProductStatus.DRAFT) {
            throw new IllegalStateException("Chỉ product ở trạng thái DRAFT mới được gửi duyệt");
        }

        product.setStatus(ProductStatus.PENDING);

        ProductPublishingHistory history = new ProductPublishingHistory();

        history.setProductId(product.getId());
        history.setFromStatus(oldStatus);
        history.setToStatus(ProductStatus.PENDING);
        history.setChangedAt(LocalDateTime.now());

        productPublishingHistoryRepository.save(history);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse activate(Long id) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        ProductStatus oldStatus = product.getStatus();

        if (oldStatus != ProductStatus.PENDING) {
            throw new IllegalStateException("Chỉ product ở trạng thái PENDING mới được activate");
        }

        product.setStatus(ProductStatus.ACTIVE);

        ProductPublishingHistory history = new ProductPublishingHistory();

        history.setProductId(product.getId());
        history.setFromStatus(oldStatus);
        history.setToStatus(ProductStatus.ACTIVE);
        history.setChangedAt(LocalDateTime.now());

        productPublishingHistoryRepository.save(history);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse discontinue(Long id) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy product với id: " + id));

        ProductStatus oldStatus = product.getStatus();

        if (oldStatus != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ product ở trạng thái ACTIVE mới được discontinue");
        }

        product.setStatus(ProductStatus.DISCONTINUED);

        ProductPublishingHistory history = new ProductPublishingHistory();

        history.setProductId(product.getId());
        history.setFromStatus(oldStatus);
        history.setToStatus(ProductStatus.DISCONTINUED);
        history.setChangedAt(LocalDateTime.now());

        productPublishingHistoryRepository.save(history);

        return productMapper.toResponse(product);
    }
}