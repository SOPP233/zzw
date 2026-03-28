import { createRouter, createWebHistory } from "vue-router";
import Layout from "../layout/index.vue";
import { hasRouteAccess, useAuthStore } from "../stores/auth";

const OrderList = () => import("../views/order/OrderList.vue");
const OrderManage = () => import("../views/order/OrderManage.vue");
const ProductionWorkbench = () => import("../views/production/ProductionWorkbench.vue");
const TaskCenter = () => import("../views/production/TaskCenter.vue");
const BasicProducts = () => import("../views/basic/BasicProducts.vue");
const BasicEquipments = () => import("../views/basic/BasicEquipments.vue");
const InventoryLedger = () => import("../views/inventory/InventoryLedger.vue");
const StatsDashboard = () => import("../views/stats/StatsDashboard.vue");
const Unauthorized = () => import("../views/system/Unauthorized.vue");

const routes = [
  {
    path: "/",
    component: Layout,
    redirect: "/orders/tracking",
    children: [
      { path: "orders/tracking", name: "OrderTracking", component: OrderList },
      { path: "orders/manage", name: "OrderManage", component: OrderManage },
      { path: "production/workbench", name: "ProductionWorkbench", component: ProductionWorkbench },
      { path: "production/tasks", name: "TaskCenter", component: TaskCenter },
      { path: "basic/products", name: "BasicProducts", component: BasicProducts },
      { path: "basic/equipments", name: "BasicEquipments", component: BasicEquipments },
      { path: "inventory/ledger", name: "InventoryLedger", component: InventoryLedger },
      { path: "stats/dashboard", name: "StatsDashboard", component: StatsDashboard }
    ]
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

router.beforeEach((to, from, next) => {
  if (to.path === "/unauthorized") {
    next();
    return;
  }
  const authStore = useAuthStore();
  if (hasRouteAccess(to.path, authStore.roleCode.value)) {
    next();
    return;
  }
  next("/unauthorized");
});

export default router;

