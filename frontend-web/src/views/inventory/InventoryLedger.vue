<template>
  <div class="page">
    <el-alert
      title="库存模块遵循单据驱动：不提供孤立库存修改入口。"
      type="warning"
      :closable="false"
      show-icon
      class="mb-12"
    />

    <el-row :gutter="12" class="mb-12">
      <el-col :span="8">
        <el-card shadow="never">
          <div class="kpi-title">原材料库存</div>
          <div class="kpi-value">{{ summary.rawMaterial }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="kpi-title">在制品库存</div>
          <div class="kpi-value">{{ summary.wip }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="kpi-title">产成品库存</div>
          <div class="kpi-value">{{ summary.finishedGoods }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="预领料单" name="issue">
        <el-table v-loading="issueLoading" :data="issueDocs" border>
          <el-table-column prop="docNo" label="单号" min-width="170" />
          <el-table-column prop="batchId" label="生产批次" min-width="140" />
          <el-table-column prop="materialType" label="物料类型" min-width="120" />
          <el-table-column prop="plannedQty" label="计划领用量" min-width="110" />
          <el-table-column prop="statusText" label="状态" min-width="110" />
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :disabled="row.status !== 'PENDING'"
                @click="approveIssue(row)"
              >
                审核发料
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="成品入库单" name="inbound">
        <el-table v-loading="inboundLoading" :data="inboundDocs" border>
          <el-table-column prop="docNo" label="单号" min-width="170" />
          <el-table-column prop="batchId" label="生产批次" min-width="140" />
          <el-table-column prop="orderId" label="关联订单" min-width="140" />
          <el-table-column prop="qualifiedQty" label="合格入库量" min-width="110" />
          <el-table-column prop="statusText" label="状态" min-width="110" />
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :disabled="row.status !== 'PENDING'"
                @click="confirmInbound(row)"
              >
                确认入库
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const activeTab = ref("issue");
const issueLoading = ref(false);
const inboundLoading = ref(false);

const summary = ref({
  rawMaterial: "-",
  wip: "-",
  finishedGoods: "-"
});

const issueDocs = ref([]);
const inboundDocs = ref([]);

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
    // 后端暂未提供时保持占位，不影响主流程
  }
};

const loadIssueDocs = async () => {
  issueLoading.value = true;
  try {
    const res = await request.get("/api/inventory/issue-docs", { params: { pageNo: 1, pageSize: 200 } });
    issueDocs.value = readList(res).map((item) => ({
      ...item,
      statusText: toStatusText(item.status)
    }));
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
    inboundDocs.value = readList(res).map((item) => ({
      ...item,
      statusText: toStatusText(item.status)
    }));
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
    ElMessage.success("发料审核成功，库存与任务状态已联动更新");
    await Promise.all([loadIssueDocs(), loadSummary()]);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "发料审核失败");
  }
};

const confirmInbound = async (row) => {
  try {
    await request.post(`/api/inventory/inbound-docs/${row.docNo}/confirm`);
    ElMessage.success("成品入库成功，库存与订单交货数量已联动更新");
    await Promise.all([loadInboundDocs(), loadSummary()]);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "确认入库失败");
  }
};

onMounted(async () => {
  await Promise.all([loadSummary(), loadIssueDocs(), loadInboundDocs()]);
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

