<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>订单综合追踪列表</span>
        </div>
      </template>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="客户名">
          <el-input
            v-model="query.customerName"
            placeholder="请输入客户名"
            clearable
            @keyup.enter="fetchOrders"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchOrders">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="orders"
        border
        row-key="orderId"
        style="width: 100%"
      >
        <el-table-column type="expand" width="56">
          <template #default="{ row }">
            <div class="expand-wrap">
              <el-table :data="row.details || []" size="small" border>
                <el-table-column prop="detailId" label="明细编号" min-width="160" />
                <el-table-column prop="productModel" label="产品型号" min-width="140" />
                <el-table-column prop="airPermeability" label="透气量" width="100" />
                <el-table-column prop="lengthReq" label="长度" width="100" />
                <el-table-column prop="widthReq" label="宽度" width="100" />
                <el-table-column prop="craftReq" label="工艺要求" min-width="150" />
                <el-table-column label="织造显示状态" min-width="120">
                  <template #default="{ row: detail }">
                    <el-tag size="small" type="info">{{ weavingModeText(detail.weavingModeStatus) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="当前工序" min-width="170">
                  <template #default="{ row: detail }">
                    <el-tag size="small" type="warning">{{ processStageText(detail) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="明细状态" min-width="120">
                  <template #default="{ row: detail }">
                    <el-tag size="small">{{ detailStatusText(detail.detailStatus) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="orderId" label="订单号" min-width="180" />
        <el-table-column prop="contractNo" label="合同号" min-width="180" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="expectedDate" label="交付时间" min-width="140" />
        <el-table-column label="总状态" min-width="120">
          <template #default="{ row }">
            <el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { ORDER_DETAIL_STATUS_MAP, ORDER_STATUS_MAP, WEAVING_MODE_STATUS_MAP } from "../../constants/order";

const loading = ref(false);
const orders = ref([]);

const query = reactive({
  customerName: ""
});

const PROCESS_TYPE_TEXT = {
  1: "织造",
  2: "定型",
  3: "裁网",
  4: "插接",
  5: "二次定型"
};

const TASK_STATUS_TEXT = {
  1: "待生产",
  2: "已完工",
  0: "待接收",
  3: "已完成"
};

const orderStatusText = (status) => ORDER_STATUS_MAP[status] || "未知";
const detailStatusText = (status) => ORDER_DETAIL_STATUS_MAP[status] || "未知";
const weavingModeText = (status) => WEAVING_MODE_STATUS_MAP[status ?? 0] || "未知";

const processStageText = (detail) => {
  const processName = PROCESS_TYPE_TEXT[detail.currentProcessType];
  const taskStatus = TASK_STATUS_TEXT[detail.currentTaskStatus];
  if (processName) {
    return taskStatus ? `${processName} / ${taskStatus}` : processName;
  }
  if (detail.detailStatus >= 2) {
    return "生产流转中";
  }
  return "未投产";
};

const normalizeOrderRows = (payload) => {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (payload?.records && Array.isArray(payload.records)) {
    return payload.records;
  }
  if (payload?.list && Array.isArray(payload.list)) {
    return payload.list;
  }
  return [];
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/orders", {
      params: {
        customerName: query.customerName || undefined
      }
    });
    orders.value = normalizeOrderRows(res?.data ?? res);
  } catch (error) {
    if (error?.response?.status === 404) {
      const fallback = await request.get("/api/order-masters", {
        params: {
          pageNo: 1,
          pageSize: 200
        }
      });
      const rows = normalizeOrderRows(fallback?.data ?? fallback).map((row) => ({
        ...row,
        customerName: row.customerName || row.customerId,
        details: row.details || []
      }));
      orders.value = rows;
      ElMessage.warning("后端未提供 /api/orders，已降级加载基础订单数据");
      return;
    }
    orders.value = [];
    ElMessage.error(error?.response?.data?.message || "加载订单数据失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.customerName = "";
  fetchOrders();
};

onMounted(() => {
  fetchOrders();
});
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
</style>
