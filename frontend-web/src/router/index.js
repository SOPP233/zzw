import { createRouter, createWebHistory } from "vue-router";
import Layout from "../layout/index.vue";
import { hasRouteAccess, useAuthStore } from "../stores/auth";

const OrderList = () => import("../views/order/OrderList.vue");
const OrderManage = () => import("../views/order/OrderManage.vue");
const ScheduleWorkbench = () => import("../views/ScheduleWorkbench.vue");
const ProductionReview = () => import("../views/production/ProductionReview.vue");
const WeavingOrders = () => import("../views/production/WeavingOrders.vue");
const SettingOrders = () => import("../views/production/SettingOrders.vue");
const CuttingOrders = () => import("../views/production/CuttingOrders.vue");
const SplicingOrders = () => import("../views/production/SplicingOrders.vue");
const SecSettingOrders = () => import("../views/production/SecSettingOrders.vue");
const BasicProducts = () => import("../views/basic/BasicProducts.vue");
const BasicEquipments = () => import("../views/basic/BasicEquipments.vue");
const SystemUserManage = () => import("../views/system/SystemUserManage.vue");
const RawMaterialInventory = () => import("../views/inventory/RawMaterialInventory.vue");
const WipInventory = () => import("../views/inventory/WipInventory.vue");
const FinishedGoodsInventory = () => import("../views/inventory/FinishedGoodsInventory.vue");
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
      { path: "production/workbench", name: "ScheduleWorkbench", component: ScheduleWorkbench },
      { path: "production/review", name: "ProductionReview", component: ProductionReview },
      { path: "production/weaving-orders", name: "WeavingOrders", component: WeavingOrders },
      { path: "production/setting-orders", name: "SettingOrders", component: SettingOrders },
      { path: "production/cutting-orders", name: "CuttingOrders", component: CuttingOrders },
      { path: "production/splicing-orders", name: "SplicingOrders", component: SplicingOrders },
      { path: "production/sec-setting-orders", name: "SecSettingOrders", component: SecSettingOrders },
      { path: "basic/products", name: "BasicProducts", component: BasicProducts },
      { path: "basic/equipments", name: "BasicEquipments", component: BasicEquipments },
      { path: "system/users", name: "SystemUserManage", component: SystemUserManage },
      { path: "inventory/ledger", name: "InventoryLedger", redirect: "/inventory/raw-materials" },
      { path: "inventory/raw-materials", name: "RawMaterialInventory", component: RawMaterialInventory },
      { path: "inventory/wip", name: "WipInventory", component: WipInventory },
      { path: "inventory/finished-goods", name: "FinishedGoodsInventory", component: FinishedGoodsInventory },
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
