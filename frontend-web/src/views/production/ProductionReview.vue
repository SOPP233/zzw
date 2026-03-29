<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row"><span>生产审核</span></div>
      </template>

      <el-alert
        title="排产成功后进入审核区；点击审核通过后才流转织造。"
        type="info"
        :closable="false"
        show-icon
        class="mb-12"
      />

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="query.orderId" placeholder="请输入订单号" clearable @keyup.enter="fetchOrders" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="query.customerName" placeholder="请输入客户名称" clearable @keyup.enter="fetchOrders" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchOrders">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="orders" border row-key="orderId">
        <el-table-column prop="orderId" label="订单号" min-width="170" />
        <el-table-column prop="contractId" label="合同号" min-width="160" />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column prop="expectedDate" label="预期交期" min-width="120" />
        <el-table-column prop="totalAmount" label="合同金额" min-width="120" />
        <el-table-column label="当前状态" min-width="120">
          <template #default="{ row }">
            <el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link :disabled="!hasPendingReview(row)" @click="approve(row)">审核通过并流转织造</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchOrders"
          @size-change="onPageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../utils/request";
import { ORDER_STATUS_MAP } from "../../constants/order";

const loading = ref(false);
const orders = ref([]);
const total = ref(0);

const query = reactive({
  orderId: "",
  customerName: "",
  pageNo: 1,
  pageSize: 10
});

const orderStatusText = (status) => ORDER_STATUS_MAP[status] || "未知";

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) {
    return { records: payload.records, total: Number(payload.total || 0) };
  }
  if (Array.isArray(payload)) {
    return { records: payload, total: payload.length };
  }
  return { records: [], total: 0 };
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/orders/full", {
      params: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        orderId: query.orderId || undefined,
        customerName: query.customerName || undefined,
        orderStatus: 1
      }
    });
    const page = parsePage(res?.data ?? res);
    const filtered = page.records.filter((row) => hasPendingReview(row));
    orders.value = filtered;
    total.value = filtered.length;
  } catch (error) {
    orders.value = [];
    total.value = 0;
    ElMessage.error(error?.response?.data?.message || "加载生产审核订单失败");
  } finally {
    loading.value = false;
  }
};

const approve = async (row) => {
  await ElMessageBox.confirm(`确认将订单 ${row.orderId} 审核通过并流转到织造部门？`, "生产审核确认", {
    type: "warning"
  });
  try {
    await request.post(`/api/order-masters/${row.orderId}/production-review`);
    ElMessage.success("审核通过，订单已流转至织造中");
    fetchOrders();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "审核提交失败");
  }
};

const hasPendingReview = (row) => (row?.details || []).some((d) => d.detailStatus === 1);

const resetQuery = () => {
  query.orderId = "";
  query.customerName = "";
  query.pageNo = 1;
  fetchOrders();
};

const onPageSizeChange = () => {
  query.pageNo = 1;
  fetchOrders();
};

fetchOrders();
</script>

<style scoped>
.page {
  padding: 4px;
}

.header-row {
  font-size: 16px;
  font-weight: 600;
}

.mb-12 {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
