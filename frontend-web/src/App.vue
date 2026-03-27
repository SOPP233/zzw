<template>
  <main class="container">
    <h1>LW Web（Vue 3）</h1>
    <p>后端地址：{{ apiBaseUrl }}</p>
    <button @click="checkHealth" :disabled="loading">
      {{ loading ? "请求中..." : "检查后端健康状态" }}
    </button>
    <p v-if="message">{{ message }}</p>
  </main>
</template>

<script setup lang="ts">
import { ref } from "vue";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const loading = ref(false);
const message = ref("");

const checkHealth = async () => {
  loading.value = true;
  message.value = "";
  try {
    const response = await fetch(`${apiBaseUrl}/api/health`);
    const data = await response.json();
    message.value = `后端状态：${data.status}`;
  } catch (error) {
    message.value = "后端不可用，请检查服务是否已启动。";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.container {
  max-width: 720px;
  margin: 64px auto;
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

button {
  border: 0;
  background: #0f766e;
  color: #fff;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>

