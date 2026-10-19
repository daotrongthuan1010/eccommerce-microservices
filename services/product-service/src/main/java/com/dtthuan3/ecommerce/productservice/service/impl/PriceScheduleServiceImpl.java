
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.domain.PriceSchedule;
import com.dtthuan3.ecommerce.productservice.dto.request.CreatePriceScheduleRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.PriceScheduleResponse;
import com.dtthuan3.ecommerce.productservice.mapper.PriceScheduleMapper;
import com.dtthuan3.ecommerce.productservice.repository.PriceScheduleRepository;
import com.dtthuan3.ecommerce.productservice.repository.SkuRepository;
import com.dtthuan3.ecommerce.productservice.service.PriceScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceScheduleServiceImpl implements PriceScheduleService {

    private final PriceScheduleRepository priceScheduleRepository;
    private final SkuRepository skuRepository;
    private final PriceScheduleMapper priceScheduleMapper;

    @Override
    @Transactional
    public PriceScheduleResponse create(
            UUID skuId,
            CreatePriceScheduleRequest request
    ) {
        // 1. Kiểm tra SKU tồn tại
        if (!skuRepository.existsById(skuId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy SKU với ID: " + skuId
            );
        }

        // 2. Kiểm tra khoảng thời gian
        LocalDateTime effectiveFrom = request.effectiveFrom();
        LocalDateTime effectiveTo = request.effectiveTo();

        if (effectiveTo != null
                && !effectiveTo.isAfter(effectiveFrom)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Thời điểm kết thúc phải sau thời điểm bắt đầu"
            );
        }

        // 3. Chuẩn hóa tiền tệ
        String currency = request.currency() == null
                || request.currency().isBlank()
                ? "VND"
                : request.currency()
                  .trim()
                  .toUpperCase(Locale.ROOT);

        if (!"VND".equals(currency)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Hiện tại hệ thống chỉ hỗ trợ tiền tệ VND"
            );
        }

        // 4. Kiểm tra lịch giá chồng lấn
        boolean overlapping =
                priceScheduleRepository.existsOverlappingSchedule(
                        skuId,
                        effectiveFrom,
                        effectiveTo,
                        null
                );

        if (overlapping) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Khoảng thời gian hiệu lực bị trùng với lịch giá khác của SKU"
            );
        }

        // 5. Tạo lịch giá
        PriceSchedule priceSchedule = PriceSchedule.builder()
                .id(UUID.randomUUID())
                .skuId(skuId)
                .price(request.price())
                .currency(currency)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .status("PENDING")
                .productVersion(request.productVersion())
                .build();

        // 6. Lưu dữ liệu
        PriceSchedule saved =
                priceScheduleRepository.save(priceSchedule);

        // 7. Entity -> Response DTO bằng MapStruct
        return priceScheduleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceScheduleResponse> getBySkuId(UUID skuId) {
        if (!skuRepository.existsById(skuId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy SKU với ID: " + skuId
            );
        }

        List<PriceSchedule> schedules =
                priceScheduleRepository
                        .findBySkuIdOrderByEffectiveFromDesc(skuId);

        return priceScheduleMapper.toResponseList(schedules);
    }
}
