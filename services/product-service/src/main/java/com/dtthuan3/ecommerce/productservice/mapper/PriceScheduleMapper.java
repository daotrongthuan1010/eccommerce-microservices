package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.PriceSchedule;
import com.dtthuan3.ecommerce.productservice.dto.response.PriceScheduleResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PriceScheduleMapper {

    PriceScheduleResponse toResponse(PriceSchedule priceSchedule);

    List<PriceScheduleResponse> toResponseList(
            List<PriceSchedule> priceSchedules
    );
}
