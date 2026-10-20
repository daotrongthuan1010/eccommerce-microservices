package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DisplayRuleCreateRequest(

        @NotNull(message = "Loại rule không được để trống")
        DisplayRuleType ruleType,

        @NotNull(message = "Rule config không được để trống")
        Map<String, Object> ruleConfig,

        @Min(value = 0, message = "Priority không được nhỏ hơn 0")
        Integer priority,

        DisplayRuleStatus status

) {
}