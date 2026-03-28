import axios from "axios";
import { ElMessage } from "element-plus";
import { getAuthToken } from "../stores/auth";

const request = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 15000
});

request.interceptors.response.use(
  (response) => {
    const data = response.data;

    if (Object.prototype.hasOwnProperty.call(data, "code") && data.code !== 200) {
      ElMessage.error(data.message || "请求失败");
      return Promise.reject(new Error(data.message || "Request Error"));
    }

    return data;
  },
  (error) => {
    ElMessage.error(error?.response?.data?.message || error.message || "网络异常");
    return Promise.reject(error);
  }
);

request.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default request;
