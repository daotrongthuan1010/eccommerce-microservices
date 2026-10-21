package com.dtthuan3.ecommerce.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh gateway enrich header IAM (P2): goi nguoc auth-service lay AuthContext roi gan
 * X-User-Id / X-Tenant-Id / X-Permissions cho downstream. Best-effort: IAM cham/loi chi
 * log, request van di tiep khong header.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "iam.enrichment")
public class IamEnrichmentProperties {

    /** Tat de bo qua hoan toan buoc enrich (chi giu RBAC JWT nhu P0). */
    private boolean enabled = true;

    /** Base URL auth-service noi bo. Dung lb:// khi co Eureka, http://... khi goi truc tiep. */
    private String authServiceUrl = "lb://auth-service";

    /** Shared secret gui qua header X-Internal-Token (phai trung IAM_INTERNAL_TOKEN). */
    private String internalToken = "";

    /** Timeout moi lan goi getContext (ms). Qua timeout = bo enrich, khong chan request. */
    private long timeoutMs = 800;
}
