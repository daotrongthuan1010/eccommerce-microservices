import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery } from '../../app/api';

// menu-rbac (FE, realigned to BE): DB-backed IAM slice — Menu -> Action ->
// Permission (MENU.ACTION) -> Role -> UserGroup. Base paths follow the spec
// (same as BE controllers, via gateway /api -> auth-service):
//   menus  /auth/admin/menus          (MenuAdminController)
//   actions /auth/admin/actions       (ActionAdminController)
//   perms  /auth/admin/permissions     (PermissionAdminController)
//   roles  /auth/admin/roles/db       (DbRoleController; `/admin/roles` route
//          + `/auth/admin/roles` path belong to the Keycloak realm-roles UI)
//   groups /auth/admin/groups         (UserGroupAdminController)
// BE list endpoints return RAW JSON arrays (not {items,total}), and
// GET /auth/me/menus returns the envelope {menus,total} where each menu
// carries `actions` as [{code,name}] objects — both shapes are normalised
// here via transformResponse so pages keep using {items,total} / string[].
// Hai loai tai khoan (user-confirmed): (1) STAFF do cap tren tao, phan quyen
// theo chuoi Menu/Action/Permission/Role/UserGroup o day; (2) MARKETPLACE
// (buyer/seller kieu Shopee/Tiki) tu dang ky + verify mail, seller = role
// SELLER + bang phu `seller_profiles` (BE sau, ngoai pham vi change nay).
export interface IamMenu {
  id: string;
  code: string;
  name: string;
  path?: string | null;
  icon?: string | null;
  parentId?: string | null;
  sortOrder?: number;
  active: boolean;
  default?: boolean;
}

export interface IamAction {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  active: boolean;
}

export interface IamPermission {
  id: string;
  code: string;
  menuId: string;
  actionId: string;
  menuCode?: string;
  actionCode?: string;
}

export interface IamRole {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  permissionIds?: Array<string | number>;
  memberCount?: number;
}

export interface IamGroup {
  id: string;
  name: string;
  /** BE UserGroup khong co code — giu optional cho hien thi fallback. */
  code?: string | null;
  description?: string | null;
  tenantId?: number | null;
  groupTypeId?: number | null;
  parentId?: string | number | null;
  children?: IamGroup[];
  roleIds?: Array<string | number>;
  active?: boolean;
}

export interface MyMenuItem {
  code: string;
  name: string;
  path?: string | null;
  icon?: string | null;
  sortOrder?: number | null;
  actions: string[];
  children?: MyMenuItem[];
}

export interface ListWrapper<T> {
  items: T[];
  total: number;
}

const MENUS = '/auth/admin/menus';
const ACTIONS = '/auth/admin/actions';
const PERMS = '/auth/admin/permissions';
const ROLES = '/auth/admin/roles/db';
const GROUPS = '/auth/admin/groups';

interface RawMenuBE {
  id: string | number;
  name: string;
  code: string;
  path?: string | null;
  icon?: string | null;
  parentId?: string | number | null | '';
  orderNo?: number | '' | null;
  sortOrder?: number | null;
  active?: boolean;
  default?: boolean;
  isDefault?: boolean;
}

/** BE MenuAdminService.toMap() tra {id,code,name,path,icon,parentId,orderNo,
 *  active,default} trong do parentId la so hoac "", orderNo la so hoac "".
 *  Map ve IamMenu: parentId so -> string, "" -> null; orderNo -> sortOrder. */
function toIamMenu(raw: RawMenuBE): IamMenu {
  return {
    id: String(raw.id),
    code: raw.code,
    name: raw.name,
    path: (raw.path as string | null | undefined) ?? null,
    icon: (raw.icon as string | null | undefined) ?? null,
    parentId:
      raw.parentId === '' || raw.parentId == null ? null : String(raw.parentId),
    sortOrder:
      typeof raw.orderNo === 'number'
        ? raw.orderNo
        : typeof raw.sortOrder === 'number'
          ? raw.sortOrder
          : undefined,
    active: Boolean(raw.active),
    default: Boolean(raw.default ?? raw.isDefault),
  };
}

/** BE tra raw array — go ve {items,total} (giu nguyen neu BE da go envelope). */
function unwrapList<T>(data: unknown): ListWrapper<T> {
  if (Array.isArray(data)) return { items: data as T[], total: data.length };
  const w = data as ListWrapper<T> | null | undefined;
  if (w && Array.isArray(w.items)) return { items: w.items, total: w.total ?? w.items.length };
  return { items: [], total: 0 };
}

/** BE menu dung `orderNo`; FE form dung `sortOrder`. Whitelist strict vi BE
 *  (Jackson/mac dinh) 400 khi gap field la (code/active chi doc o update). */
function toMenuPayload(
  data: Partial<IamMenu> & Record<string, unknown>,
  forCreate: boolean,
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  if (forCreate) {
    out.code = data.code;
  }
  if (data.parentId !== undefined) out.parentId = data.parentId || null;
  if (data.name !== undefined) out.name = data.name;
  if (data.path !== undefined) out.path = data.path ?? null;
  if (data.icon !== undefined) out.icon = data.icon ?? null;
  const so = data.sortOrder as number | undefined;
  if (so !== undefined) out.orderNo = so;
  return out;
}

interface RawMyMenu {
  code: string;
  name: string;
  path?: string | null;
  icon?: string | null;
  sortOrder?: number | null;
  actions?: Array<string | { code: string; name?: string }> | null;
  children?: RawMyMenu[] | null;
}

/** Map 1 raw node BE (MeMenuController/IamQueryService.menusForUser) ve MyMenuItem. */
function toMyMenu(m: RawMyMenu): MyMenuItem {
  return {
    code: m.code,
    name: m.name,
    path: m.path ?? null,
    icon: m.icon ?? null,
    sortOrder: m.sortOrder ?? null,
    actions: (m.actions ?? []).map((a) => (typeof a === 'string' ? a : a.code)),
    children: Array.isArray(m.children) ? m.children.map(toMyMenu) : [],
  };
}

/** Unwrap {menus,total} (hoac raw array) + map de quy children/actions. */
function unwrapMyMenus(data: unknown): MyMenuItem[] {
  const raw: unknown = (() => {
    if (Array.isArray(data)) return data;
    const env = data as { menus?: unknown } | null | undefined;
    return env && Array.isArray(env.menus) ? env.menus : [];
  })();
  return (raw as RawMyMenu[]).map(toMyMenu);
}

export const iamApi = createApi({
  reducerPath: 'iamApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['IamMenu', 'IamAction', 'IamPermission', 'IamRole', 'IamGroup', 'MyMenus'],
  endpoints: (build) => ({
    // GET /auth/admin/menus → raw array (ref: MenuAdminController.list)
    listMenus: build.query<ListWrapper<IamMenu>, void>({
      query: () => ({ url: MENUS }),
      transformResponse: (data: unknown): ListWrapper<IamMenu> => {
        const w = unwrapList<RawMenuBE>(data);
        const items = w.items.map(toIamMenu);
        return { items, total: w.total };
      },
      providesTags: ['IamMenu'],
    }),
    // GET /auth/admin/menus/{id} (ref: MenuAdminController.get)
    getMenu: build.query<IamMenu, string>({
      query: (id) => ({ url: `${MENUS}/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'IamMenu', id }],
    }),
    // POST /auth/admin/menus {code,name,path?,icon?,parentId?,orderNo?}
    createMenu: build.mutation<IamMenu, Partial<IamMenu> & { code: string; name: string }>({
      query: (data) => ({ url: MENUS, method: 'post', data: toMenuPayload(data, true) }),
      invalidatesTags: ['IamMenu', 'MyMenus'],
    }),
    // PUT /auth/admin/menus/{id} {name?,path?,icon?,orderNo?,parentId?} (code/active la read-only, parentId null = ve root)
    updateMenu: build.mutation<IamMenu, { id: string; data: Partial<IamMenu> }>({
      query: ({ id, data }) => ({ url: `${MENUS}/${id}`, method: 'put', data: toMenuPayload(data, false) }),
      invalidatesTags: ['IamMenu', 'MyMenus'],
    }),
    // DELETE /auth/admin/menus/{id}
    deleteMenu: build.mutation<{ deleted: boolean }, string>({
      query: (id) => ({ url: `${MENUS}/${id}`, method: 'delete' }),
      invalidatesTags: ['IamMenu', 'IamPermission', 'MyMenus'],
    }),
    // GET /auth/admin/actions → raw array (ref: ActionAdminController list)
    listActions: build.query<ListWrapper<IamAction>, void>({
      query: () => ({ url: ACTIONS }),
      transformResponse: unwrapList<IamAction>,
      providesTags: ['IamAction'],
    }),
    // POST /auth/admin/actions {code,name}
    createAction: build.mutation<IamAction, { code: string; name: string }>({
      query: (data) => ({ url: ACTIONS, method: 'post', data }),
      invalidatesTags: ['IamAction'],
    }),
    // PUT /auth/admin/actions/{id} {name?,description?} — code immutable, strip code/active
    updateAction: build.mutation<IamAction, { id: string; data: Partial<IamAction> }>({
      query: ({ id, data }) => {
        const payload: Record<string, unknown> = {};
        if (data.name !== undefined) payload.name = data.name;
        const desc = (data as Record<string, unknown>).description;
        if (desc !== undefined) payload.description = desc;
        return { url: `${ACTIONS}/${id}`, method: 'put', data: payload };
      },
      invalidatesTags: ['IamAction', 'IamPermission'],
    }),
    // PATCH /auth/admin/actions/{id}/toggle → {active}
    toggleAction: build.mutation<IamAction, string>({
      query: (id) => ({ url: `${ACTIONS}/${id}/toggle`, method: 'patch' }),
      invalidatesTags: ['IamAction', 'IamPermission'],
    }),
    // GET /auth/admin/permissions (all) | GET /menu/{menuId} (by menu)
    // (ref: PermissionAdminController list + byMenu)
    listPermissions: build.query<ListWrapper<IamPermission>, { menuId?: string } | void>({
      query: (params) =>
        params?.menuId ? { url: `${PERMS}/menu/${params.menuId}` } : { url: PERMS },
      transformResponse: unwrapList<IamPermission>,
      providesTags: ['IamPermission'],
    }),
    // POST /auth/admin/permissions {menuId,actionId} → code = MENU.ACTION
    createPermission: build.mutation<IamPermission, { menuId: string; actionId: string }>({
      query: (data) => ({ url: PERMS, method: 'post', data }),
      invalidatesTags: ['IamPermission', 'IamRole', 'MyMenus'],
    }),
    // DELETE /auth/admin/permissions/{id}
    deletePermission: build.mutation<{ deleted: boolean }, string>({
      query: (id) => ({ url: `${PERMS}/${id}`, method: 'delete' }),
      invalidatesTags: ['IamPermission', 'IamRole', 'MyMenus'],
    }),
    // GET /auth/admin/roles/db → raw array (ref: DbRoleController list)
    listIamRoles: build.query<ListWrapper<IamRole>, void>({
      query: () => ({ url: ROLES }),
      transformResponse: unwrapList<IamRole>,
      providesTags: ['IamRole'],
    }),
    // POST /auth/admin/roles/db {code,name,description?,permissionIds?}
    createIamRole: build.mutation<IamRole, { code: string; name: string; description?: string; permissionIds?: string[] }>({
      query: (data) => ({ url: ROLES, method: 'post', data }),
      invalidatesTags: ['IamRole'],
    }),
    // DELETE /auth/admin/roles/db/{id}
    deleteIamRole: build.mutation<{ deleted: boolean }, string>({
      query: (id) => ({ url: `${ROLES}/${id}`, method: 'delete' }),
      invalidatesTags: ['IamRole', 'IamGroup', 'MyMenus'],
    }),
    // PUT /auth/admin/roles/db/{id}/permissions — BE nhan RAW array [1,2,3]
    // (Set<Long>), khong phai object {permissionIds}. Ep ve number.
    setRolePermissions: build.mutation<IamRole, { id: string; permissionIds: Array<string | number> }>({
      query: ({ id, permissionIds }) => ({
        url: `${ROLES}/${id}/permissions`,
        method: 'put',
        data: permissionIds.map((p) => Number(p)),
      }),
      invalidatesTags: (_r, _e, { id }) => ['IamRole', 'MyMenus', { type: 'IamRole', id }],
    }),
    // GET /auth/admin/groups[?tenantId] → raw array (ref: UserGroupAdminController)
    listGroups: build.query<ListWrapper<IamGroup>, { tenantId?: number } | void>({
      query: (params) => ({
        url: GROUPS,
        params: params?.tenantId != null ? { tenantId: params.tenantId } : {},
      }),
      transformResponse: unwrapList<IamGroup>,
      providesTags: ['IamGroup'],
    }),
    listGroupTree: build.query<IamGroup[], { tenantId?: number } | void>({
      query: (params) => ({
        url: `${GROUPS}/tree`,
        params: params?.tenantId != null ? { tenantId: params.tenantId } : {},
      }),
      providesTags: ['IamGroup'],
    }),
    // POST /auth/admin/groups {tenantId!,name,description?,groupTypeId?,parentId?}
    createGroup: build.mutation<
      IamGroup,
      { name: string; tenantId: number; description?: string; groupTypeId?: number | null; parentId?: string | number | null }
    >({
      query: (data) => ({
        url: GROUPS,
        method: 'post',
        data: {
          tenantId: data.tenantId,
          name: data.name,
          description: data.description ?? null,
          groupTypeId: data.groupTypeId ?? null,
          parentId: data.parentId == null || data.parentId === '' ? null : Number(data.parentId),
        },
      }),
      invalidatesTags: ['IamGroup'],
    }),
    // PUT /auth/admin/groups/{id} {name?,description?,active?} (khong doi parent)
    updateGroup: build.mutation<
      IamGroup,
      { id: string; data: { name?: string; description?: string; active?: boolean } }
    >({
      query: ({ id, data }) => ({ url: `${GROUPS}/${id}`, method: 'put', data }),
      invalidatesTags: ['IamGroup'],
    }),
    // DELETE /auth/admin/groups/{id}
    deleteGroup: build.mutation<{ deleted: boolean }, string>({
      query: (id) => ({ url: `${GROUPS}/${id}`, method: 'delete' }),
      invalidatesTags: ['IamGroup', 'MyMenus'],
    }),
    // Gan/go role cho group (BE tao moi cho menu-rbac).
    // POST /auth/admin/groups/{id}/roles/{roleId} + DELETE
    assignGroupRole: build.mutation<{ assigned: boolean }, { groupId: string; roleId: string }>({
      query: ({ groupId, roleId }) => ({
        url: `${GROUPS}/${groupId}/roles/${roleId}`,
        method: 'post',
      }),
      invalidatesTags: ['IamGroup', 'MyMenus'],
    }),
    removeGroupRole: build.mutation<{ revoked: boolean }, { groupId: string; roleId: string }>({
      query: ({ groupId, roleId }) => ({
        url: `${GROUPS}/${groupId}/roles/${roleId}`,
        method: 'delete',
      }),
      invalidatesTags: ['IamGroup', 'MyMenus'],
    }),
    // GET /auth/me/menus {menus,total} — sider dong theo quyen merge tu group
    // (ref: MeMenuController). transformResponse unwrap + map actions ve codes.
    getMyMenus: build.query<MyMenuItem[], void>({
      query: () => ({ url: '/auth/me/menus' }),
      transformResponse: unwrapMyMenus,
      providesTags: ['MyMenus'],
    }),
    // GET /auth/me/permissions → string[] codes (ref: MeMenuController)
    getMyPermissions: build.query<string[], void>({
      query: () => ({ url: '/auth/me/permissions' }),
      transformResponse: (data: unknown): string[] => {
        if (Array.isArray(data)) return data as string[];
        const env = data as { permissions?: unknown } | null | undefined;
        return env && Array.isArray(env.permissions) ? (env.permissions as string[]) : [];
      },
      providesTags: ['MyMenus'],
    }),
  }),
});

export const {
  useListMenusQuery,
  useGetMenuQuery,
  useCreateMenuMutation,
  useUpdateMenuMutation,
  useDeleteMenuMutation,
  useListActionsQuery,
  useCreateActionMutation,
  useUpdateActionMutation,
  useToggleActionMutation,
  useListPermissionsQuery,
  useCreatePermissionMutation,
  useDeletePermissionMutation,
  useListIamRolesQuery,
  useCreateIamRoleMutation,
  useDeleteIamRoleMutation,
  useSetRolePermissionsMutation,
  useListGroupsQuery,
  useListGroupTreeQuery,
  useCreateGroupMutation,
  useUpdateGroupMutation,
  useDeleteGroupMutation,
  useAssignGroupRoleMutation,
  useRemoveGroupRoleMutation,
  useGetMyMenusQuery,
  useGetMyPermissionsQuery,
} = iamApi;
