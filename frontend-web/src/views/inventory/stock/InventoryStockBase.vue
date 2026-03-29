<template>
  <div class="page">
    <el-alert
      :title="alertText"
      type="warning"
      :closable="false"
      show-icon
      class="mb-12"
    />

    <el-card shadow="never" class="mb-12">
      <div class="kpi-title">{{ kpiTitle }}</div>
      <div class="kpi-value">{{ kpiValue }}</div>
    </el-card>

    <el-card v-if="showIssueTable" shadow="never" class="mb-12">
      <template #header>
        <span>预领料单</span>
      </template>
      <el-table v-loading="issueLoading" :data="issueDocs" border>
        <el-table-column prop="docNo" label="单号" min-width="170" />
        <el-table-column prop="batchId" label="生产批次" min-width="140" />
        <el-table-column prop="materialType" label="物料类型" min-width="120" />
        <el-table-column prop="plannedQty" label="计划领用量" min-width="110" />
        <el-table-column prop="statusText" label="状态" min-width="110" />
        <el-table-column v-if="canApproveIssue" label="操作" width="130">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="row.status !== 'PENDING'" @click="approveIssue(row)">
              审核发料
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="showInboundTable" shadow="never">
      <template #header>
        <span>成品入库单</span>
      </template>
      <el-table v-loading="inboundLoading" :data="inboundDocs" border>
        <el-table-column prop="docNo" label="单号" min-width="170" />
        <el-table-column prop="batchId" label="生产批次" min-width="140" />
        <el-table-column prop="orderId" label="关联订单" min-width="140" />
        <el-table-column prop="qualifiedQty" label="合格入库量" min-width="110" />
        <el-table-column prop="statusText" label="状态" min-width="110" />
        <el-table-column v-if="canConfirmInbound" label="操作" width="130">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="row.status !== 'PENDING'" @click="confirmInbound(row)">
              确认入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../../utils/request";

const props = defineProps({
  pageType: {
    type: String,
    required: true
  }
});

const issueLoading = ref(false);
const inboundLoading = ref(false);
const summary = ref({ rawMaterial: "-", wip: "-", finishedGoods: "-" });
const issueDocs = ref([]);
const inboundDocs = ref([]);

const showIssueTable = computed(() => props.pageType === "raw" || props.pageType === "wip");
const showInboundTable = computed(() => props.pageType === "wip" || props.pageType === "finished");
const canApproveIssue = computed(() => props.pageType === "raw");
const canConfirmInbound = computed(() => props.pageType === "finished");

const kpiTitle = computed(() => {
  if (props.pageType === "raw") return "原材料库库存";
  if (props.pageType === "wip") return "半成品库库存";
  return "成品库库存";
});

const kpiValue = computed(() => {
  if (props.pageType === "raw") return summary.value.rawMaterial;
  if (props.pageType === "wip") return summary.value.wip;
  return summary.value.finishedGoods;
});

const alertText = computed(() => {
  if (props.pageType === "raw") return "原材料库：仅可通过预领料单审核触发出库，不提供手工改库存入口。";
  if (props.pageType === "wip") return "半成品库：由工序流转自动沉淀，展示预领料与入库单据进度。";
  return "成品库：仅可通过成品入库单确认触发入库，不提供孤立库存变更。";
});

const readData = (res) => res?.data ?? res;
const readList = (res) => {
  const payload = readData(res);
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.records)) return payload.records;
  if (Array.isArray(payload?.list)) return payload.list;
  return [];
};

const toStatusText = (status) => {
  const map = {
    PENDING: "待审核",
    APPROVED: "已完成",
    REJECTED: "已驳回"
  };
  return map[status] || status || "-";
};

const loadSummary = async () => {
  try {
    const res = await request.get("/api/inventory/summary");
    const data = readData(res) || {};
    summary.value = {
      rawMaterial: data.rawMaterial ?? "-",
      wip: data.wip ?? "-",
      finishedGoods: data.finishedGoods ?? "-"
    };
  } catch {
    // keep placeholder
  }
};

const loadIssueDocs = async () => {
  issueLoading.value = true;
  try {
    const res = await request.get("/api/inventory/issue-docs", { params: { pageNo: 1, pageSize: 200 } });
    issueDocs.value = readList(res).map((item) => ({ ...item, statusText: toStatusText(item.status) }));
  } catch (error) {
    issueDocs.value = [];
    ElMessage.warning(error?.response?.status === 404 ? "预领料单接口待接入" : "加载预领料单失败");
  } finally {
    issueLoading.value = false;
  }
};

const loadInboundDocs = async () => {
  inboundLoading.value = true;
  try {
    const res = await request.get("/api/inventory/inbound-docs", { params: { pageNo: 1, pageSize: 200 } });
    inboundDocs.value = readList(res).map((item) => ({ ...item, statusText: toStatusText(item.status) }));
  } catch (error) {
    inboundDocs.value = [];
    ElMessage.warning(error?.response?.status === 404 ? "成品入库单接口待接入" : "加载成品入库单失败");
  } finally {
    inboundLoading.value = false;
  }
};

const approveIssue = async (row) => {
  try {
    await request.post(`/api/inventory/issue-docs/${row.docNo}/approve`);
    ElMessage.success("发料审核成功");
    await Promise.all([loadIssueDocs(), loadSummary()]);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "发料审核失败");
  }
};

const confirmInbound = async (row) => {
  try {
    await request.post(`/api/inventory/inbound-docs/${row.docNo}/confirm`);
    ElMessage.success("成品入库成功");
    await Promise.all([loadInboundDocs(), loadSummary()]);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "确认入库失败");
  }
};

onMounted(async () => {
  const jobs = [loadSummary()];
  if (showIssueTable.value) jobs.push(loadIssueDocs());
  if (showInboundTable.value) jobs.push(loadInboundDocs());
  await Promise.all(jobs);
});
</script>

<style scoped>
.page {
  padding: 4px;
}

.mb-12 {
  margin-bottom: 12px;
}

.kpi-title {
  color: #6b7280;
  font-size: 13px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}
</style>
