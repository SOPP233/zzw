<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">MES Web</div>
      <el-menu :default-active="$route.path" router>
        <el-sub-menu v-if="visible('/orders/tracking') || visible('/orders/manage')" index="/orders">
          <template #title>订单管理</template>
          <el-menu-item v-if="visible('/orders/tracking')" index="/orders/tracking">订单综合追踪</el-menu-item>
          <el-menu-item v-if="visible('/orders/manage')" index="/orders/manage">订单维护</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visible('/production/workbench') || visible('/production/tasks')" index="/production">
          <template #title>生产协同</template>
          <el-menu-item v-if="visible('/production/workbench')" index="/production/workbench">排产工作台</el-menu-item>
          <el-menu-item v-if="visible('/production/tasks')" index="/production/tasks">工序任务中心</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visible('/basic/products') || visible('/basic/equipments')" index="/basic">
          <template #title>基础数据</template>
          <el-menu-item v-if="visible('/basic/products')" index="/basic/products">产品与工艺路线</el-menu-item>
          <el-menu-item v-if="visible('/basic/equipments')" index="/basic/equipments">设备与产能模型</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="visible('/inventory/ledger')" index="/inventory/ledger">库存管理</el-menu-item>
        <el-menu-item v-if="visible('/stats/dashboard')" index="/stats/dashboard">数据统计</el-menu-item>
      </el-menu>
    </aside>

    <section class="main">
      <header class="header">
        <div class="title">离散制造 MES 管理端</div>
        <div class="role-switch">
          <span class="role-label">当前角色</span>
          <el-select v-model="roleCodeProxy" size="small" style="width: 150px">
            <el-option v-for="role in ROLE_OPTIONS" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ROLE_OPTIONS } from "../constants/rbac";
import { useAuthStore } from "../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const visible = (path) => authStore.hasRouteAccess(path, authStore.roleCode.value);

const roleCodeProxy = computed({
  get: () => authStore.roleCode.value,
  set: (value) => {
    authStore.setRoleCode(value);
    const target = authStore.getDefaultRouteByRole(value);
    if (route.path !== target) {
      router.push(target);
    }
  }
});
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: #f5f7fa;
}

.sider {
  background: #0f172a;
  color: #fff;
  padding-top: 16px;
  min-height: 100vh;
}

.brand {
  padding: 0 18px 18px;
  font-size: 18px;
  font-weight: 700;
}

.main {
  display: flex;
  flex-direction: column;
}

.header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.role-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-label {
  color: #4b5563;
  font-size: 13px;
}

.content {
  padding: 16px;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  color: #d1d5db;
}

:deep(.el-menu-item.is-active) {
  color: #ffffff;
  background-color: #1f2937;
}
</style>

