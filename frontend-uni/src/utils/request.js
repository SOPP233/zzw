const BASE_URL = "http://localhost:8080";

function getToken() {
  return uni.getStorageSync("token") || "";
}

function parseErrorMessage(payload) {
  if (!payload) return "请求失败";
  if (typeof payload === "string") return payload;
  return payload.message || payload.msg || payload.error || "请求失败";
}

export function request(options) {
  const { url, method = "GET", data, auth = true } = options;
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: auth
        ? {
            Authorization: `Bearer ${getToken()}`
          }
        : {},
      success: (res) => {
        const body = res.data;
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (body && body.success === false) {
            reject(new Error(parseErrorMessage(body)));
            return;
          }
          resolve(body && Object.prototype.hasOwnProperty.call(body, "data") ? body.data : body);
          return;
        }
        reject(new Error(parseErrorMessage(body)));
      },
      fail: (err) => {
        reject(new Error(err?.errMsg || "网络异常"));
      }
    });
  });
}

export function clearAuth() {
  uni.removeStorageSync("token");
  uni.removeStorageSync("userInfo");
}
