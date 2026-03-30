import { computed, reactive } from "vue";
import axios from "axios";
import { DEFAULT_ROUTE_BY_ROLE, ROLE_CODE, ROUTE_ROLE_MAP } from "../constants/rbac";

const ROLE_STORAGE_KEY = "mes_role_code";
const TOKEN_STORAGE_KEY = "mes_auth_token";
const USER_STORAGE_KEY = "mes_auth_user";

const authRequest = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 15000
});

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

  const login = async ({ username: loginName, password }) => {
    const res = await authRequest.post("/api/auth/login", {
      username: loginName,
      password
    });
    const payload = res?.data?.data ?? res?.data ?? {};
    const roleCodeFromApi = payload.roleCode || ROLE_CODE.SALES;
    const usernameFromApi = payload.username || loginName;
    const tokenFromApi = payload.token || "";

    if (!tokenFromApi) {
      return { success: false, message: "登录失败，未获取到令牌" };
    }
    state.roleCode = roleCodeFromApi;
    state.username = usernameFromApi;
    state.token = tokenFromApi;
    localStorage.setItem(ROLE_STORAGE_KEY, roleCodeFromApi);
    localStorage.setItem(USER_STORAGE_KEY, usernameFromApi);
    localStorage.setItem(TOKEN_STORAGE_KEY, tokenFromApi);
    return { success: true };
  };

  const logout = async () => {
    try {
      if (state.token) {
        await authRequest.post(
          "/api/auth/logout",
          {},
          { headers: { Authorization: `Bearer ${state.token}` } }
        );
      }
    } catch (_) {
      // ignore network failures on logout
    }
    state.token = "";
    state.username = "";
    state.roleCode = ROLE_CODE.SALES;
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    localStorage.removeItem(ROLE_STORAGE_KEY);
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
