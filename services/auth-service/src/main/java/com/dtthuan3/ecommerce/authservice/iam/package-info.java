/**
 * IAM DB (tenant / group / role / menu / action / permission / AuthContext) - lop dieu khien
 * kich hoat tu change {@code menu-rbac} (2026-10-23).
 *
 * <p>Mo hinh phan quyen nhan su (top-down, admin cap tren cap tai khoan):
 * {@code Menu} -&gt; {@code Action} -&gt; {@code Permission} (menu+action, code {@code MENU.ACTION})
 * -&gt; {@code Role} -&gt; {@code UserGroup} (thanh vien thua ke qua {@code UserGroupRole}).
 * Keycloak realm {@code ecommerce} van la identity source duy nhat ({@code sub} = {@code AuthUser.id}).
 *
 * <p>Web layer (ADMIN-only, tru {@code /auth/me/menus} authenticated):
 *
 * <ul>
 *   <li>Menu/Action CRUD: {@code MenuAdminController}, {@code ActionAdminController} (409 khi trung
 *       code, action chi toggle khong xoa cung).
 *   <li>Permission: {@code PermissionAdminController} (POST{menuId,actionId}, 409 khi trung cap;
 *       {@code GET /actions?menuCode=} loc theo grant cua caller).
 *   <li>DB Role + Group: {@code DbRoleController}, {@code UserGroupAdminController} (assign/revoke/
 *       setPermissions + POST/DELETE {@code /groups/{id}/roles/{roleId}} moi; self-lockout guard
 *       409 qua {@code IamSelfLockout}).
 *   <li>Truy van tong hop: {@code EffectivePermissionController} (KEYCLOAK UNION DB_DIRECT/DB_GROUP
 *       kem source tags), {@code MeMenuController} (sider dong, sysadmin thay tat ca).
 *   <li>Marketplace tu dang ky: {@code POST /auth/register} (USER + SELLER option, auto
 *       VERIFY_EMAIL, 201 + verificationRequired; SMTP la dieu kien tien quyet, thieu thi
 *       verificationPending). {@code seller_profiles}/CCCD la side table tuong lai (ngoai scope).
 * </ul>
 *
 * <p>Che do cu: P0 giu RBAC don gian theo Keycloak JWT; P2 kich hoat IAM DB bo sung (JIT
 * provisioning, gateway enrich header, invalidation). Xem {@code IamProvisioningService}.
 *
 * <p>Ghi chu: chua chuyen sang Authorization Code + PKCE - cho frontend that, giu password
 * grant proxy hien tai (xem AuthController).
 */
package com.dtthuan3.ecommerce.authservice.iam;
