<template>
  <view class="container">
    <view class="title">LW 小程序（uni-app）</view>
    <view class="desc">后端地址：{{ apiBaseUrl }}</view>
    <button class="btn" type="primary" @click="checkHealth" :disabled="loading">
      {{ loading ? "请求中..." : "检查后端健康状态" }}
    </button>
    <view v-if="message" class="message">{{ message }}</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { API_BASE_URL } from "../../config";

const apiBaseUrl = API_BASE_URL;
const loading = ref(false);
const message = ref("");

const checkHealth = () => {
  loading.value = true;
  message.value = "";
  uni.request({
    url: `${apiBaseUrl}/api/health`,
    method: "GET",
    success: (res) => {
      const data = res.data as { status?: string };
      message.value = `后端状态：${data.status || "unknown"}`;
    },
    fail: () => {
      message.value = "后端不可用，请检查服务是否已启动。";
    },
    complete: () => {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.container {
  padding: 32rpx;
}

.title {
  font-size: 38rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16rpx;
}

.desc {
  font-size: 28rpx;
  color: #6b7280;
  margin-bottom: 20rpx;
}

.btn {
  margin-top: 10rpx;
}

.message {
  margin-top: 22rpx;
  font-size: 28rpx;
  color: #111827;
}
</style>

