package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturerResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import com.dtthuan3.ecommerce.catalogservice.mapper.ManufacturerMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.ManufacturersRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.ManufacturersService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManufacturerServiceImpl
        implements ManufacturersService {

    private final ManufacturersRepository manufacturersRepository;

    private final ManufacturerMapper manufacturerMapper;

    private final RedisCacheService redisCacheService;




    @Override
    @Transactional
    public ManufacturerResponse create(
            ManufacturerCreateRequest request
    ) {

        String name = normalizeName(
                request.getName()
        );

        if (manufacturersRepository
                .existsByNameIgnoreCase(name)) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Tên nhà sản xuất đã tồn tại"
            );
        }

        Manufacturers manufacturers =
                manufacturerMapper.toEntity(request);

        manufacturers.setName(name);

        if (manufacturers.getStatus() == null) {
            manufacturers.setStatus(
                    ManufacturerStatus.ACTIVE
            );
        }

        if (manufacturers.getRevision() == null) {
            manufacturers.setRevision(0L);
        }

        Manufacturers saved =
                manufacturersRepository.save(
                        manufacturers
                );

        evictManufacturerListCaches();

        return manufacturerMapper.toResponse(saved);
    }



    @Override
    public ManufacturerResponse getById(
            String id
    ) {

        String cacheKey =
                CacheKeys.MANUFACTURER + id;

        Optional<ManufacturerResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        ManufacturerResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Manufacturers manufacturers =
                findManufacturer(id);

        ManufacturerResponse response =
                manufacturerMapper.toResponse(
                        manufacturers
                );

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }



    @Override
    public PageResponse<ManufacturerResponse> getManufacturers(
            ManufacturerStatus status,
            String name,
            int page,
            int size
    ) {

        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Page không được nhỏ hơn 0"
            );
        }

        if (size <= 0 || size > 100) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Size phải nằm trong khoảng từ 1 đến 100"
            );
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("name"),
                                Sort.Order.asc("id")
                        )
                );

        Page<Manufacturers> result;

        if (status != null) {

            result =
                    manufacturersRepository.findByStatus(
                            status,
                            pageable
                    );

        } else if (name != null
                && !name.isBlank()) {

            result =
                    manufacturersRepository
                            .findByNameContainingIgnoreCase(
                                    name.trim(),
                                    pageable
                            );

        } else {

            result =
                    manufacturersRepository.findAll(
                            pageable
                    );
        }

        List<ManufacturerResponse> content =
                result.getContent()
                        .stream()
                        .map(manufacturerMapper::toResponse)
                        .toList();

        return PageResponse.of(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }



    @Override
    @Transactional
    public ManufacturerResponse update(
            String id,
            ManufacturerUpdateRequest request
    ) {

        Manufacturers manufacturers =
                findManufacturer(id);

        String name =
                normalizeName(request.getName());

        if (manufacturersRepository
                .existsByNameIgnoreCaseAndIdNot(
                        name,
                        id
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Tên nhà sản xuất đã tồn tại"
            );
        }

        manufacturerMapper.updateEntity(
                request,
                manufacturers
        );

        // Đảm bảo name đã normalize
        manufacturers.setName(name);

        Long currentRevision =
                manufacturers.getRevision();

        if (currentRevision == null) {
            currentRevision = 0L;
        }

        manufacturers.setRevision(
                currentRevision + 1
        );

        Manufacturers saved =
                manufacturersRepository.save(
                        manufacturers
                );

        redisCacheService.delete(
                CacheKeys.MANUFACTURER + id
        );

        evictManufacturerListCaches();

        return manufacturerMapper.toResponse(
                saved
        );
    }



    @Override
    @Transactional
    public void delete(
            String id
    ) {

        Manufacturers manufacturers =
                findManufacturer(id);

        if (manufacturers.getStatus()
                != ManufacturerStatus.ARCHIVED) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Chỉ được xóa nhà sản xuất ở trạng thái ARCHIVED"
            );
        }

        manufacturersRepository.delete(
                manufacturers
        );

        redisCacheService.delete(
                CacheKeys.MANUFACTURER + id
        );

        evictManufacturerListCaches();
    }



    @Override
    @Transactional
    public ManufacturerResponse updateStatus(
            String id,
            ManufacturerStatus status
    ) {

        if (status == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Trạng thái không được để trống"
            );
        }

        Manufacturers manufacturers =
                findManufacturer(id);

        manufacturers.setStatus(status);

        Long currentRevision =
                manufacturers.getRevision();

        if (currentRevision == null) {
            currentRevision = 0L;
        }

        manufacturers.setRevision(
                currentRevision + 1
        );

        Manufacturers saved =
                manufacturersRepository.save(
                        manufacturers
                );

        redisCacheService.delete(
                CacheKeys.MANUFACTURER + id
        );

        evictManufacturerListCaches();

        return manufacturerMapper.toResponse(
                saved
        );
    }



    @Override
    public List<ManufacturerResponse>
    getActiveManufacturers() {

        String cacheKey =
                CacheKeys.MANUFACTURER_ACTIVE;

        List<ManufacturerResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        ManufacturerResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<ManufacturerResponse> result =
                manufacturersRepository
                        .findByStatusOrderByNameAsc(
                                ManufacturerStatus.ACTIVE
                        )
                        .stream()
                        .map(manufacturerMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }



    @Override
    public List<ManufacturerResponse> search(
            String keyword
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return List.of();
        }

        String normalizedKeyword =
                keyword.trim().toLowerCase();

        String cacheKey =
                CacheKeys.MANUFACTURER_SEARCH
                        + normalizedKeyword;

        List<ManufacturerResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        ManufacturerResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<ManufacturerResponse> result =
                manufacturersRepository
                        .findByNameContainingIgnoreCase(
                                normalizedKeyword,
                                PageRequest.of(
                                        0,
                                        20,
                                        Sort.by("name")
                                                .ascending()
                                )
                        )
                        .getContent()
                        .stream()
                        .map(manufacturerMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }



    private Manufacturers findManufacturer(
            String id
    ) {

        return manufacturersRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy nhà sản xuất: " + id
                        )
                );
    }



    private String normalizeName(
            String name
    ) {

        if (name == null) {
            return null;
        }

        return name.trim();
    }



    private void evictManufacturerListCaches() {

        redisCacheService.delete(
                CacheKeys.MANUFACTURER_LIST
        );

        redisCacheService.delete(
                CacheKeys.MANUFACTURER_ACTIVE
        );

        redisCacheService.deleteByPattern(
                CacheKeys.MANUFACTURER_SEARCH + "*"
        );
    }
}