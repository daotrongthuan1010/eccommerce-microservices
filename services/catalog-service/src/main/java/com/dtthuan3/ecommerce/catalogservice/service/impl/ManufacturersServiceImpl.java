package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturersRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturersRespone;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import com.dtthuan3.ecommerce.catalogservice.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.catalogservice.mapper.ManufacturersMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.ManufacturersRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.ManufacturersService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManufacturersServiceImpl
        implements ManufacturersService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final ManufacturersRepository manufacturersRepository;

    private final ManufacturersMapper manufacturersMapper;

    private final RedisCacheService redisCacheService;

    @Override
    public ManufacturersRespone create(
            ManufacturersRequest request
    ) {

        validateNameForCreate(request.getName());

        Manufacturers manufacturers =
                manufacturersMapper.toEntity(request);

        Manufacturers savedManufacturers =
                manufacturersRepository.save( manufacturers);

        return toResponse(savedManufacturers);
    }

    @Override
    @Transactional(readOnly = true)
    public ManufacturersRespone getById(Long id) {

        String cacheKey = CacheKeys.MANUFACTURER + id;

        var cached = redisCacheService.get(
                        cacheKey,
                        ManufacturersRespone.class
                );

        if (cached.isPresent()) {return cached.get();}

        Manufacturers manufacturers = findManufacturerById(id);

        ManufacturersRespone response = toResponse(manufacturers);

        redisCacheService.set(cacheKey, response, CACHE_TTL);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManufacturersRespone> getAll() {

        return manufacturersRepository
                .findAllByStatusOrderByNameAsc(
                        ManufacturerStatus.ACTIVE
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ManufacturersRespone update(
            Long id,
            ManufacturersRequest request
    ) {

        Manufacturers manufacturers = findManufacturerById(id);

        validateNameForUpdate(request.getName(), id);

        manufacturersMapper.updateEntity(manufacturers, request);

        Manufacturers updatedManufacturers =
                manufacturersRepository.save(
                        manufacturers
                );

        redisCacheService.delete(CacheKeys.MANUFACTURER + id);

        return toResponse(updatedManufacturers);
    }

    @Override
    public void delete(Long id) {

        Manufacturers manufacturers = findManufacturerById(id);

        manufacturersRepository.delete(manufacturers);

        redisCacheService.delete(CacheKeys.MANUFACTURER + id);
    }

    private Manufacturers findManufacturerById(Long id) {

        return manufacturersRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy nhà sản xuất với id: "
                                        + id
                        )
                );
    }

    private void validateNameForCreate(
            String name
    ) {

        if (manufacturersRepository
                        .existsByNameIgnoreCase(name)
        ) {

            throw new IllegalArgumentException(
                    "Tên nhà sản xuất đã tồn tại: "
                            + name
            );
        }
    }

    private void validateNameForUpdate(
            String name,
            Long id
    ) {

        if (
                manufacturersRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                name,
                                id
                        )
        ) {

            throw new IllegalArgumentException(
                    "Tên nhà sản xuất đã tồn tại: "
                            + name
            );
        }
    }

    private ManufacturersRespone toResponse(
            Manufacturers manufacturers
    ) {

        return manufacturersMapper.toResponse(
                manufacturers
        );
    }
}