import { computed, reactive } from "vue";
import { DEFAULT_ROUTE_BY_ROLE, ROLE_CODE, ROUTE_ROLE_MAP } from "../constants/rbac";

const ROLE_STORAGE_KEY = "mes_role_code";
const TOKEN_STORAGE_KEY = "mes_auth_token";
const USER_STORAGE_KEY = "mes_auth_user";

const state = reactive({
  roleCode: localStorage.getItem(ROLE_STORAGE_KEY) || ROLE_CODE.SALES,
  token: localStorage.getItem(TOKEN_STORAGE_KEY) || "",
  username: localStorage.getItem(USER_STORAGE_KEY) || ""
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

const LOGIN_USER_MAP = {
  admin: { password: "admin123", roleCode: ROLE_CODE.SYSTEM_ADMIN, displayName: "系统管理员" },
  leader: { password: "leader123", roleCode: ROLE_CODE.LEADER, displayName: "企业领导" },
  sales: { password: "sales123", roleCode: ROLE_CODE.SALES, displayName: "销售员" },
  director: { password: "director123", roleCode: ROLE_CODE.DIRECTOR, displayName: "车间主任" },
  worker: { password: "worker123", roleCode: ROLE_CODE.WORKER, displayName: "一线员工" },
  inventory: { password: "inventory123", roleCode: ROLE_CODE.INVENTORY_ADMIN, displayName: "库存管理员" }
};

export const getAuthToken = () => state.token || localStorage.getItem(TOKEN_STORAGE_KEY) || "";

export function useAuthStore() {
  const roleCode = computed(() => state.roleCode);
  const token = computed(() => state.token);
  const username = computed(() => state.username);
  const isAuthenticated = computed(() => Boolean(state.token));

  const setRoleCode = (nextRoleCode) => {
    state.roleCode = nextRoleCode;
    localStorage.setItem(ROLE_STORAGE_KEY, nextRoleCode);
  };

  const login = ({ username: loginName, password }) => {
    const record = LOGIN_USER_MAP[(loginName || "").trim().toLowerCase()];
    if (!record || record.password !== password) {
      return { success: false, message: "用户名或密码错误" };
    }
    const nextToken = `MES-${Date.now()}-${record.roleCode}`;
    state.roleCode = record.roleCode;
    state.username = record.displayName;
    state.token = nextToken;
    localStorage.setItem(ROLE_STORAGE_KEY, record.roleCode);
    localStorage.setItem(USER_STORAGE_KEY, record.displayName);
    localStorage.setItem(TOKEN_STORAGE_KEY, nextToken);
    return { success: true };
  };

  const logout = () => {
    state.token = "";
    state.username = "";
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  };

  return {
    roleCode,
    token,
    username,
    isAuthenticated,
    setRoleCode,
    login,
    logout,
    hasRouteAccess,
    getDefaultRouteByRole
  };
}
