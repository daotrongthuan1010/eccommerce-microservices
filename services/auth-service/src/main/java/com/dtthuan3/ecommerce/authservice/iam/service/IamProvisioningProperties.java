package com.dtthuan3.ecommerce.authservice.iam.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh JIT provisioning (P2): user dang nhap bang Keycloak lan dau se duoc tao dong
 * tu dong trong bang auth_users. Neu khong cau hinh tenant/group mac dinh, user duoc tao
 * o muc he thong (tenant = null, group = null) va khong co quyen gi cho den khi admin gan.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "iam.provision")
public class IamProvisioningProperties {

    /** Tat de vo hieu hoa hoan toan JIT (getContext chi doc DB). */
    private boolean enabled = true;

    /** Ma tenant mac dinh gan cho user moi (vd DEFAULT). Bo trong = khong gan tenant. */
    private String defaultTenantCode = "";

    /** Id group mac dinh gan cho user moi. Null = khong gan group. */
    private Long defaultGroupId;
}
