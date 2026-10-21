package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleType;
import jakarta.validation.constraints.Min;

import java.util.Map;

public record DisplayRuleUpdateRequest(

        DisplayRuleType ruleType,

        Map<String, Object> ruleConfig,

        @Min(value = 0, message = "Priority không được nhỏ hơn 0")
        Integer priority,

        DisplayRuleStatus status

) {
}