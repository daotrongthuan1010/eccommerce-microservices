package com.dtthuan3.ecommerce.apigateway.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subset AuthContext tu auth-service, chi du de enrich header - khong map full DTO
 * de gateway khong vo khi auth-service them field.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IamContextView {

    private String userId;
    private Long tenantId;
    private String tenantCode;
    private Set<String> permissions;
}
