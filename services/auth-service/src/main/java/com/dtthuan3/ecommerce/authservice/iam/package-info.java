/**
 * IAM DB (tenant / group / role / permission / AuthContext) - kich hoat tu P2 (2026-10-21).
 *
 * <p>Quyet dinh P0: giu RBAC don gian theo Keycloak JWT (realm_access.roles +
 * resource_access roles -&gt; ROLE_*, scope/scp -&gt; SCOPE_*). Khong chay song song 2 he quyen
 * tren duong dan chinh.
 *
 * <p>P2 kich hoat IAM DB o che do bo sung (khong chan request):
 *
 * <ul>
 *   <li>JIT provisioning: {@code IamProvisioningService} tao {@code auth_users} khi thay
 *       Keycloak sub la lan dau (claims tu JWT: sub, preferred_username, email), gan
 *       tenant/group mac dinh neu co seed, idempotent + chong trung username/email.
 *   <li>Gateway enrich header (best-effort): lay AuthContext qua
 *       {@code InternalAuthController} va gan X-User-Id / X-Tenant-Id / X-Permissions cho
 *       downstream. IAM loi chi log, khong chan request.
 *   <li>Invalidation: xoa {@code iam:ctx:*} khi gan/thu hoi role (endpoint
 *       {@code /auth/admin/users/{userId}/context/refresh} + tu dong khi provision).
 *   <li>Duong internal van khong route public: gateway denyAll moi path internal (P0 giu
 *       nguyen); service-to-service dung shared secret X-Internal-Token.
 * </ul>
 *
 * <p>Ghi chu: chua chuyen sang Authorization Code + PKCE - cho frontend that, giu password
 * grant proxy hien tai (xem AuthController).
 */
package com.dtthuan3.ecommerce.authservice.iam;
