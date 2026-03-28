<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>订单管理</span>
        </div>
      </template>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="query.orderId" placeholder="请输入订单号" clearable @keyup.enter="fetchOrders" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" placeholder="请输入客户名" clearable @keyup.enter="fetchOrders" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.orderStatus" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="opt in ORDER_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchOrders">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="orders" border row-key="orderId">
        <el-table-column type="expand" width="56">
          <template #default="{ row }">
            <div class="expand-wrap">
              <el-table :data="row.details || []" size="small" border>
                <el-table-column prop="detailId" label="明细编号" min-width="150" />
                <el-table-column prop="productModel" label="产品型号" min-width="130" />
                <el-table-column prop="lengthReq" label="长度" width="90" />
                <el-table-column prop="widthReq" label="宽度" width="90" />
                <el-table-column prop="requiredQty" label="需求数量" width="110" />
                <el-table-column prop="deliveryDate" label="交付期限" min-width="120" />
                <el-table-column label="明细状态" min-width="120">
                  <template #default="{ row: detail }">
                    <el-tag size="small">{{ detailStatusText(detail.detailStatus) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="orderId" label="订单号" min-width="170" />
        <el-table-column prop="customerName" label="客户" min-width="130" />
        <el-table-column prop="signDate" label="签订日期" min-width="120" />
        <el-table-column prop="totalAmount" label="合同总金额" min-width="120" />
        <el-table-column prop="deliveryAddress" label="交付地址" min-width="180" />
        <el-table-column label="订单状态" min-width="120">
          <template #default="{ row }">
            <el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openLogDrawer(row)">流转日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="logDrawerVisible" :title="`订单流转日志 - ${activeOrderId}`" size="36%">
      <el-timeline v-if="logs.length > 0">
        <el-timeline-item
          v-for="item in logs"
          :key="item.logId || `${item.operateTime}-${item.operatorId}`"
          :timestamp="item.operateTime"
        >
          <div class="log-title">{{ item.fromStatusText }} -> {{ item.toStatusText }}</div>
          <div class="log-meta">操作人：{{ item.operatorId }}</div>
          <div class="log-meta" v-if="item.remark">备注：{{ item.remark }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流转日志" />
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { ORDER_DETAIL_STATUS_MAP, ORDER_STATUS_MAP, ORDER_STATUS_OPTIONS } from "../../constants/order";

const loading = ref(false);
const orders = ref([]);
const logs = ref([]);
const logDrawerVisible = ref(false);
const activeOrderId = ref("");

const query = reactive({
  orderId: "",
  customerName: "",
  orderStatus: undefined
});

const orderStatusText = (status) => ORDER_STATUS_MAP[status] || "未知";
const detailStatusText = (status) => ORDER_DETAIL_STATUS_MAP[status] || "未知";

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    // API 骨架：推荐后端提供 /api/orders/full，返回主表+明细+客户名等聚合数据
    const res = await request.get("/api/orders/full", {
      params: {
        orderId: query.orderId || undefined,
        customerName: query.customerName || undefined,
        orderStatus: query.orderStatus
      }
    });
    orders.value = normalizeRows(res?.data ?? res);
  } catch (error) {
    // 兼容后端尚未提供聚合接口的阶段，降级到基础订单表
    if (error?.response?.status === 404) {
      const fallback = await request.get("/api/order-masters", {
        params: { pageNo: 1, pageSize: 200 }
      });
      const rows = normalizeRows(fallback?.data ?? fallback).map((row) => ({
        ...row,
        signDate: row.signDate || row.createdAt?.slice?.(0, 10) || "",
        deliveryAddress: row.deliveryAddress || "",
        details: row.details || []
      }));
      orders.value = rows;
      ElMessage.warning("后端未提供 /api/orders/full，当前显示基础订单数据");
      return;
    }
    orders.value = [];
    ElMessage.error(error?.response?.data?.message || "订单数据加载失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.orderId = "";
  query.customerName = "";
  query.orderStatus = undefined;
  fetchOrders();
};

const openLogDrawer = async (row) => {
  activeOrderId.value = row.orderId;
  logDrawerVisible.value = true;
  logs.value = [];
  try {
    // API 骨架：订单状态流转日志
    const res = await request.get("/api/order-flow-logs", {
      params: { orderId: row.orderId }
    });
    const list = normalizeRows(res?.data ?? res);
    logs.value = list.map((item) => ({
      ...item,
      fromStatusText: orderStatusText(item.fromStatus),
      toStatusText: orderStatusText(item.toStatus)
    }));
  } catch (error) {
    if (error?.response?.status === 404) {
      ElMessage.warning("后端暂未提供流转日志接口 /api/order-flow-logs");
      return;
    }
    ElMessage.error(error?.response?.data?.message || "加载流转日志失败");
  }
};

fetchOrders();
</script>

<style scoped>
.page {
  padding: 4px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  margin-bottom: 12px;
}

.expand-wrap {
  padding: 8px 12px;
  background: #fafafa;
}

.log-title {
  font-weight: 600;
  color: #111827;
}

.log-meta {
  color: #6b7280;
  margin-top: 4px;
}
</style>

