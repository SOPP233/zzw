import { computed, reactive } from "vue";
import { DEFAULT_ROUTE_BY_ROLE, ROLE_CODE, ROUTE_ROLE_MAP } from "../constants/rbac";

const STORAGE_KEY = "mes_role_code";

const state = reactive({
  roleCode: localStorage.getItem(STORAGE_KEY) || ROLE_CODE.SALES
});

const normalizePath = (path) => {
  if (!path) return "/";
  return path.startsWith("/") ? path : `/${path}`;
};

export const hasRouteAccess = (path, roleCode = state.roleCode) => {
  const routePath = normalizePath(path);
  const allowed = ROUTE_ROLE_MAP[routePath];
  if (!allowed) return true;
  return allowed.includes(roleCode);
};

export const getDefaultRouteByRole = (roleCode = state.roleCode) =>
  DEFAULT_ROUTE_BY_ROLE[roleCode] || "/orders/tracking";

export function useAuthStore() {
  const roleCode = computed(() => state.roleCode);

  const setRoleCode = (nextRoleCode) => {
    state.roleCode = nextRoleCode;
    localStorage.setItem(STORAGE_KEY, nextRoleCode);
  };

  return {
    roleCode,
    setRoleCode,
    hasRouteAccess,
    getDefaultRouteByRole
  };
}

