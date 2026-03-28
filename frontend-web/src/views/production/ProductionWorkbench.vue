<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>排产工作台</span>
        </div>
      </template>

      <el-alert
        title="合批规则：仅允许待排产且产品型号一致的订单明细合并到同一批次"
        type="info"
        :closable="false"
        show-icon
        class="mb-12"
      />

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="产品型号">
          <el-input v-model="query.productModel" placeholder="输入型号筛选" clearable @keyup.enter="fetchDetails" />
        </el-form-item>
        <el-form-item label="机台号">
          <el-input v-model="machineId" placeholder="例如 MC-01" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchDetails">刷新待排产明细</el-button>
          <el-button type="success" :disabled="selectedIds.length === 0 || !machineId" @click="handleMerge">
            合并排产
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="details"
        border
        row-key="detailId"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="detailId" label="明细编号" min-width="150" />
        <el-table-column prop="orderId" label="订单号" min-width="150" />
        <el-table-column prop="productModel" label="产品型号" min-width="140" />
        <el-table-column prop="lengthReq" label="长度" width="90" />
        <el-table-column prop="widthReq" label="宽度" width="90" />
        <el-table-column label="明细状态" min-width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ detailStatusText(row.detailStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="tip-box">
        <span>已选 {{ selectedIds.length }} 条明细</span>
        <span class="ml-12">说明：大额紧急订单拆批场景建议后续接入 `/api/schedule/split` 专用接口。</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { DETAIL_STATUS_MAP } from "../../constants/production";

const loading = ref(false);
const details = ref([]);
const selectedIds = ref([]);
const machineId = ref("");

const query = reactive({
  productModel: ""
});

const detailStatusText = (status) => DETAIL_STATUS_MAP[status] || "未知";

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const fetchDetails = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/order-details", { params: { pageNo: 1, pageSize: 500 } });
    const rows = normalizeRows(res?.data ?? res)
      .filter((item) => item.detailStatus === 2)
      .filter((item) => (query.productModel ? item.productModel?.includes(query.productModel) : true));
    details.value = rows;
    selectedIds.value = [];
  } catch (error) {
    details.value = [];
    ElMessage.error(error?.response?.data?.message || "加载待排产明细失败");
  } finally {
    loading.value = false;
  }
};

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.detailId);
};

const handleMerge = async () => {
  if (!machineId.value) {
    ElMessage.warning("请先填写机台号");
    return;
  }
  if (selectedIds.value.length < 1) {
    ElMessage.warning("请至少选择一条明细");
    return;
  }
  try {
    const res = await request.post("/api/schedule/merge", {
      detailIds: selectedIds.value,
      machineId: machineId.value
    });
    const data = res?.data ?? res;
    ElMessage.success(`排产成功，批次号：${data.batchId || "-"}`);
    fetchDetails();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "合并排产失败");
  }
};

fetchDetails();
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

.mb-12 {
  margin-bottom: 12px;
}

.tip-box {
  margin-top: 12px;
  color: #6b7280;
  font-size: 13px;
}

.ml-12 {
  margin-left: 12px;
}
</style>

