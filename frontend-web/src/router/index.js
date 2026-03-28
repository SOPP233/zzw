import { createRouter, createWebHistory } from "vue-router";
import Layout from "../layout/index.vue";
import { hasRouteAccess, useAuthStore } from "../stores/auth";

const OrderList = () => import("../views/order/OrderList.vue");
const OrderManage = () => import("../views/order/OrderManage.vue");
const ProductionWorkbench = () => import("../views/production/ProductionWorkbench.vue");
const ProductionReview = () => import("../views/production/ProductionReview.vue");
const WeavingTaskCenter = () => import("../views/production/task-center/WeavingTaskCenter.vue");
const SettingTaskCenter = () => import("../views/production/task-center/SettingTaskCenter.vue");
const CuttingTaskCenter = () => import("../views/production/task-center/CuttingTaskCenter.vue");
const JointingTaskCenter = () => import("../views/production/task-center/JointingTaskCenter.vue");
const ReshapingTaskCenter = () => import("../views/production/task-center/ReshapingTaskCenter.vue");
const BasicProducts = () => import("../views/basic/BasicProducts.vue");
const BasicEquipments = () => import("../views/basic/BasicEquipments.vue");
const InventoryLedger = () => import("../views/inventory/InventoryLedger.vue");
const StatsDashboard = () => import("../views/stats/StatsDashboard.vue");
const Unauthorized = () => import("../views/system/Unauthorized.vue");
const Login = () => import("../views/system/Login.vue");

const routes = [
  {
    path: "/",
    component: Layout,
    redirect: "/orders/tracking",
    children: [
      { path: "orders/tracking", name: "OrderTracking", component: OrderList },
      { path: "orders/manage", name: "OrderManage", component: OrderManage },
      { path: "production/workbench", name: "ProductionWorkbench", component: ProductionWorkbench },
      { path: "production/review", name: "ProductionReview", component: ProductionReview },
      { path: "production/tasks", name: "TaskCenter", redirect: "/production/tasks/weaving" },
      { path: "production/tasks/weaving", name: "WeavingTaskCenter", component: WeavingTaskCenter },
      { path: "production/tasks/setting", name: "SettingTaskCenter", component: SettingTaskCenter },
      { path: "production/tasks/cutting", name: "CuttingTaskCenter", component: CuttingTaskCenter },
      { path: "production/tasks/jointing", name: "JointingTaskCenter", component: JointingTaskCenter },
      { path: "production/tasks/reshaping", name: "ReshapingTaskCenter", component: ReshapingTaskCenter },
      { path: "basic/products", name: "BasicProducts", component: BasicProducts },
      { path: "basic/equipments", name: "BasicEquipments", component: BasicEquipments },
      { path: "inventory/ledger", name: "InventoryLedger", component: InventoryLedger },
      { path: "stats/dashboard", name: "StatsDashboard", component: StatsDashboard }
    ]
  },
  {
    path: "/login",
    name: "Login",
    component: Login
  },
  {
    path: "/unauthorized",
    name: "Unauthorized",
    component: Unauthorized
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.path === "/login") {
    if (authStore.isAuthenticated.value) {
      return authStore.getDefaultRouteByRole(authStore.roleCode.value);
    }
    return true;
  }

  if (to.path === "/unauthorized") {
    return true;
  }

  if (!authStore.isAuthenticated.value) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (hasRouteAccess(to.path, authStore.roleCode.value)) {
    return true;
  }
  return "/unauthorized";
});

export default router;
