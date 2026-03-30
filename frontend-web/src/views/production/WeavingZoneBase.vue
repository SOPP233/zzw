<template>
  <el-card>
    <template #header>
      <span>{{ title }}</span>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="织造批号">
        <el-input v-model.trim="query.batchNo" placeholder="请输入织造批号" clearable @keyup.enter="fetchRows" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchRows">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="pageRows" border>
      <el-table-column prop="weaving_batch_no" label="织造批号" min-width="170" />
      <el-table-column prop="machine_id" label="机台" width="100" />
      <el-table-column prop="actual_length" label="实际产出(m)" width="120" />
      <el-table-column prop="actual_width" label="织造宽度(m)" width="120" />
      <el-table-column prop="process_status" label="状态" width="100" />
      <el-table-column prop="completed_at" label="完工时间" min-width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="mode === 'production'" type="primary" link @click="openReport(row)">报工</el-button>
          <el-button v-else type="success" link @click="openDetail(row)">查看报工详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="pager.pageNo"
        v-model:page-size="pager.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="filteredRows.length"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </el-card>

  <el-dialog v-if="mode === 'production'" v-model="reportDialogVisible" title="织造报工" width="620px" destroy-on-close>
    <el-form :model="reportForm" label-width="120px">
      <el-form-item label="织造批号">
        <el-input :model-value="reportForm.weavingBatchNo" disabled />
      </el-form-item>
      <el-form-item label="机台号" required><el-input v-model.trim="reportForm.machineId" /></el-form-item>
      <el-form-item label="操作工号" required><el-input v-model.trim="reportForm.operatorId" /></el-form-item>
      <el-form-item label="原料批次号" required><el-input v-model.trim="reportForm.materialBatchNo" /></el-form-item>
      <el-form-item label="张力参数" required><el-input v-model.trim="reportForm.tensionParams" /></el-form-item>
      <el-form-item label="实际产出长度(m)" required>
        <el-input-number v-model="reportForm.actualLength" :min="0" :precision="2" :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="实际开工时间" required>
        <el-date-picker v-model="reportForm.actualStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item label="完工时间"><el-input model-value="提交报工时自动生成当前时间戳" disabled /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reportDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交报工</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailDialogVisible" title="报工详情" width="620px" destroy-on-close>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="织造批号">{{ detailData.weaving_batch_no || "-" }}</el-descriptions-item>
      <el-descriptions-item label="机台号">{{ detailData.machine_id || "-" }}</el-descriptions-item>
      <el-descriptions-item label="操作工号">{{ detailData.operator_id || "-" }}</el-descriptions-item>
      <el-descriptions-item label="原料批次号">{{ detailData.material_batch_no || "-" }}</el-descriptions-item>
      <el-descriptions-item label="张力参数">{{ detailData.tension_params || "-" }}</el-descriptions-item>
      <el-descriptions-item label="实际产出长度(m)">{{ detailData.actual_length ?? "-" }}</el-descriptions-item>
      <el-descriptions-item label="实际开工时间">{{ detailData.actual_start_time || "-" }}</el-descriptions-item>
      <el-descriptions-item label="完工时间">{{ detailData.actual_end_time || detailData.completed_at || "-" }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const props = defineProps({
  mode: { type: String, default: "production" } // production | completed
});

const title = computed(() => (props.mode === "production" ? "织造生产区" : "织造完工区"));
const loading = ref(false);
const rows = ref([]);
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const detailDialogVisible = ref(false);
const detailData = ref({});

const query = reactive({ batchNo: "" });
const pager = reactive({ pageNo: 1, pageSize: 10 });

const reportForm = reactive({
  weavingBatchNo: "",
  machineId: "",
  operatorId: "",
  materialBatchNo: "",
  tensionParams: "",
  actualLength: null,
  actualStartTime: ""
});

const parseData = (payload) => payload?.data ?? payload ?? {};
const parsePage = (payload) => (payload?.records && Array.isArray(payload.records) ? { records: payload.records } : { records: [] });

const filteredRows = computed(() =>
  rows.value.filter((r) => (props.mode === "production" ? Number(r.process_status) !== 2 : Number(r.process_status) === 2))
);
const pageRows = computed(() => {
  const start = (pager.pageNo - 1) * pager.pageSize;
  return filteredRows.value.slice(start, start + pager.pageSize);
});

const loadReportData = async (batchNo) => parseData(await request.get(`/api/production/weaving-orders/${batchNo}/report`));

const fetchRows = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/production/weaving-orders", { params: { pageNo: 1, pageSize: 2000, batchNo: query.batchNo || undefined } });
    rows.value = parsePage(parseData(res)).records;
    pager.pageNo = 1;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载织造订单失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.batchNo = "";
  fetchRows();
};

const openReport = async (row) => {
  reportForm.weavingBatchNo = row.weaving_batch_no;
  reportForm.machineId = row.machine_id || "";
  reportForm.operatorId = row.operator_id || "";
  reportForm.materialBatchNo = "";
  reportForm.tensionParams = "";
  reportForm.actualLength = row.actual_length ?? null;
  reportForm.actualStartTime = "";
  try {
    const report = await loadReportData(row.weaving_batch_no);
    reportForm.machineId = report.machine_id || reportForm.machineId;
    reportForm.operatorId = report.operator_id || reportForm.operatorId;
    reportForm.materialBatchNo = report.material_batch_no || "";
    reportForm.tensionParams = report.tension_params || "";
    reportForm.actualLength = report.actual_length ?? reportForm.actualLength;
    reportForm.actualStartTime = report.actual_start_time || "";
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载报工信息失败");
    return;
  }
  reportDialogVisible.value = true;
};

const openDetail = async (row) => {
  try {
    detailData.value = { ...row, ...(await loadReportData(row.weaving_batch_no)) };
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载报工详情失败");
  }
};

const submitReport = async () => {
  if (!reportForm.machineId) return ElMessage.warning("请填写机台号");
  if (!reportForm.operatorId) return ElMessage.warning("请填写操作工号");
  if (!reportForm.materialBatchNo) return ElMessage.warning("请填写原料批次号");
  if (!reportForm.tensionParams) return ElMessage.warning("请填写张力参数");
  if (reportForm.actualLength === null || reportForm.actualLength === undefined) return ElMessage.warning("请填写实际产出长度");
  if (!reportForm.actualStartTime) return ElMessage.warning("请填写实际开工时间");

  reportSubmitting.value = true;
  try {
    await request.post(`/api/production/weaving-orders/${reportForm.weavingBatchNo}/report`, {
      machineId: reportForm.machineId,
      operatorId: reportForm.operatorId,
      materialBatchNo: reportForm.materialBatchNo,
      tensionParams: reportForm.tensionParams,
      actualLength: reportForm.actualLength,
      actualStartTime: reportForm.actualStartTime
    });
    ElMessage.success("织造报工已提交");
    reportDialogVisible.value = false;
    fetchRows();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "提交报工失败");
  } finally {
    reportSubmitting.value = false;
  }
};

fetchRows();
</script>

<style scoped>
.search-bar { margin-bottom: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
