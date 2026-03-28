<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">MES 系统登录</h1>
      <p class="subtitle">请输入账号与密码进入系统</p>

      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model.trim="form.username" placeholder="admin / sales / director..." />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="full-width" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>

      <p class="hint">示例账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loading = ref(false);
const form = reactive({
  username: "",
  password: ""
});

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }
  loading.value = true;
  try {
    const result = authStore.login({ username: form.username, password: form.password });
    if (!result.success) {
      ElMessage.error(result.message);
      return;
    }
    const redirect = route.query.redirect;
    const target = typeof redirect === "string" && redirect ? redirect : authStore.getDefaultRouteByRole(authStore.roleCode.value);
    router.replace(target);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(120deg, #eff6ff 0%, #f8fafc 60%, #fef3c7 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 14px;
  padding: 28px 24px 18px;
  box-shadow: 0 12px 36px rgba(17, 24, 39, 0.12);
}

.title {
  margin: 0 0 6px;
  font-size: 24px;
  color: #111827;
}

.subtitle {
  margin: 0 0 18px;
  color: #6b7280;
}

.full-width {
  width: 100%;
}

.hint {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}
</style>
