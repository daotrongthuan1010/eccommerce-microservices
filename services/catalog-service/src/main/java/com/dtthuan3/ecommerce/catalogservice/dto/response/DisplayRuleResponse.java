package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleType;

import java.util.Map;

public record DisplayRuleResponse(

        String id,

        String collectionId,

        DisplayRuleType ruleType,

        Map<String, Object> ruleConfig,

        Integer priority,

        DisplayRuleStatus status,

        Long revision

) {
}