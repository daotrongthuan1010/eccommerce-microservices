package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.DisplayRuleResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.DisplayRule;
import org.springframework.stereotype.Component;

@Component
public class DisplayRuleMapper {

    public DisplayRule toEntity(
            DisplayRuleCreateRequest request
    ) {

        DisplayRule rule = new DisplayRule();

        rule.setRuleType(request.ruleType());
        rule.setRuleConfig(request.ruleConfig());

        rule.setPriority(
                request.priority() != null
                        ? request.priority()
                        : 0
        );

        rule.setStatus(request.status());

        return rule;
    }

    public DisplayRuleResponse toResponse(
            DisplayRule rule
    ) {

        return new DisplayRuleResponse(
                rule.getId(),
                rule.getCollectionId(),
                rule.getRuleType(),
                rule.getRuleConfig(),
                rule.getPriority(),
                rule.getStatus(),
                rule.getRevision()
        );
    }
}
