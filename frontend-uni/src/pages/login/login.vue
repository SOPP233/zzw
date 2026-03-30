<template>
  <view class="page">
    <view class="title">系统登录</view>
    <view class="subtitle">请输入账号和密码后进入报工工作台</view>
    <input v-model="form.username" placeholder="请输入账号" class="input" />
    <input v-model="form.password" placeholder="请输入密码" password class="input" />
    <button class="btn" type="primary" @click="submitLogin" :disabled="loading">
      {{ loading ? "登录中..." : "登录并进入报工" }}
    </button>
  </view>
</template>

<script setup>
import { reactive, ref } from "vue";
import { request, clearAuth } from "../../utils/request";

const form = reactive({
  username: "",
  password: ""
});
const loading = ref(false);

async function submitLogin() {
  if (!form.username || !form.password) {
    uni.showToast({ title: "请输入账号和密码", icon: "none" });
    return;
  }
  loading.value = true;
  try {
    const data = await request({
      url: "/api/auth/login",
      method: "POST",
      auth: false,
      data: {
        username: form.username.trim(),
        password: form.password
      }
    });
    uni.setStorageSync("token", data.token || "");
    uni.setStorageSync("userInfo", {
      userId: data.userId || "",
      username: data.username || form.username.trim(),
      roleCode: data.roleCode || ""
    });
    uni.reLaunch({ url: "/pages/task-center/index" });
  } catch (error) {
    clearAuth();
    uni.showToast({ title: error.message || "登录失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.page { padding: 24px; }
.title { font-size: 22px; font-weight: 700; margin-bottom: 12px; }
.subtitle { color: #6b7280; margin-bottom: 16px; font-size: 13px; }
.input { border: 1px solid #d1d5db; border-radius: 8px; padding: 10px 12px; margin-bottom: 12px; }
.btn { margin-top: 12px; }
</style>