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

        <el-sub-menu
          v-if="
            visible('/production/workbench') ||
            visible('/production/review') ||
            visible('/production/weaving-orders') ||
            visible('/production/setting-orders') ||
            visible('/production/cutting-orders') ||
            visible('/production/splicing-orders') ||
            visible('/production/sec-setting-orders')
          "
          index="/production"
        >
          <template #title>生产协同</template>
          <el-menu-item v-if="visible('/production/workbench')" index="/production/workbench">排产工作区</el-menu-item>
          <el-menu-item v-if="visible('/production/review')" index="/production/review">生产审核</el-menu-item>
          <el-menu-item v-if="visible('/production/weaving-orders')" index="/production/weaving-orders">织造订单</el-menu-item>
          <el-menu-item v-if="visible('/production/setting-orders')" index="/production/setting-orders">定型订单</el-menu-item>
          <el-menu-item v-if="visible('/production/cutting-orders')" index="/production/cutting-orders">裁网订单</el-menu-item>
          <el-menu-item v-if="visible('/production/splicing-orders')" index="/production/splicing-orders">插接订单</el-menu-item>
          <el-menu-item v-if="visible('/production/sec-setting-orders')" index="/production/sec-setting-orders">二次定型订单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visible('/basic/products') || visible('/basic/equipments')" index="/basic">
          <template #title>基础数据</template>
          <el-menu-item v-if="visible('/basic/products')" index="/basic/products">产品与工艺路线</el-menu-item>
          <el-menu-item v-if="visible('/basic/equipments')" index="/basic/equipments">设备与产能模型</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visible('/system/users')" index="/system">
          <template #title>系统管理</template>
          <el-menu-item v-if="visible('/system/users')" index="/system/users">用户管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu
          v-if="
            visible('/inventory/raw-materials') ||
            visible('/inventory/wip') ||
            visible('/inventory/finished-goods') ||
            visible('/inventory/ledger')
          "
          index="/inventory"
        >
          <template #title>库存管理</template>
          <el-menu-item v-if="visible('/inventory/raw-materials')" index="/inventory/raw-materials">原材料库</el-menu-item>
          <el-menu-item v-if="visible('/inventory/wip')" index="/inventory/wip">半成品库</el-menu-item>
          <el-menu-item v-if="visible('/inventory/finished-goods')" index="/inventory/finished-goods">成品库</el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="visible('/stats/dashboard')" index="/stats/dashboard">数据统计</el-menu-item>
      </el-menu>
    </aside>

    <section class="main">
      <header class="header">
        <div class="title">离散制造 MES 管理端</div>
        <div class="user-panel">
          <span class="user-name">{{ authStore.username.value || "未命名用户" }}</span>
          <el-button size="small" text @click="handleLogout">退出登录</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

const visible = (path) => authStore.hasRouteAccess(path, authStore.roleCode.value);

const handleLogout = async () => {
  await authStore.logout();
  router.replace("/login");
};
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

.user-panel {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
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
