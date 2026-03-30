<template>
  <el-card>
    <template #header>
      <span>织造订单</span>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="织造批号">
        <el-input v-model.trim="query.batchNo" placeholder="请输入织造批号" clearable @keyup.enter="fetchRows" />
      </el-form-item>
      <el-form-item label="状态">
        <el-input-number v-model="query.processStatus" :min="1" :controls="false" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchRows">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="生产区" name="production">
        <el-table v-loading="loading" :data="pendingPageRows" border>
          <el-table-column prop="weaving_batch_no" label="织造批号" min-width="170" />
          <el-table-column prop="machine_id" label="机台" width="100" />
          <el-table-column prop="actual_length" label="织造长度(m)" width="120" />
          <el-table-column prop="actual_width" label="织造宽度(m)" width="120" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openReport(row)">报工</el-button>
              <el-button type="success" link @click="openRelatedOrders(row)">相关订单</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="section-pager">
          <el-pagination
            v-model:current-page="pendingPager.pageNo"
            v-model:page-size="pendingPager.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pendingRows.length"
            :page-sizes="[5, 10, 20, 50]"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="完工区" name="completed">
        <el-table v-loading="loading" :data="completedPageRows" border>
          <el-table-column prop="weaving_batch_no" label="织造批号" min-width="170" />
          <el-table-column prop="machine_id" label="机台" width="100" />
          <el-table-column prop="actual_length" label="实际产出(m)" width="120" />
          <el-table-column prop="completed_at" label="完工时间" min-width="170" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link @click="openDetail(row)">查看报工详情</el-button>
              <el-button type="primary" link @click="openRelatedOrders(row)">相关订单</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="section-pager">
          <el-pagination
            v-model:current-page="completedPager.pageNo"
            v-model:page-size="completedPager.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="completedRows.length"
            :page-sizes="[5, 10, 20, 50]"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

  </el-card>

  <el-dialog v-model="reportDialogVisible" title="织造报工" width="620px" destroy-on-close>
    <el-form :model="reportForm" label-width="120px">
      <el-form-item label="织造批号">
        <el-input :model-value="reportForm.weavingBatchNo" disabled />
      </el-form-item>
      <el-form-item label="机台号" required>
        <el-input v-model.trim="reportForm.machineId" />
      </el-form-item>
      <el-form-item label="操作工号" required>
        <el-input v-model.trim="reportForm.operatorId" />
      </el-form-item>
      <el-form-item label="原料批次号" required>
        <el-input v-model.trim="reportForm.materialBatchNo" />
      </el-form-item>
      <el-form-item label="张力参数" required>
        <el-input v-model.trim="reportForm.tensionParams" />
      </el-form-item>
      <el-form-item label="实际产出长度(m)" required>
        <el-input-number v-model="reportForm.actualLength" :min="0" :precision="2" :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="实际开工时间" required>
        <el-date-picker
          v-model="reportForm.actualStartTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="完工时间">
        <el-input model-value="提交报工时自动生成当前时间戳" disabled />
      </el-form-item>
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

  <el-dialog v-model="relatedDialogVisible" title="相关订单" width="960px" destroy-on-close>
    <el-alert :title="`织造批号：${relatedBatchNo || '-'}`" type="info" :closable="false" class="related-tip" />
    <el-table v-loading="relatedLoading" :data="relatedOrders" border>
      <el-table-column prop="order_no" label="订单号" min-width="160" />
      <el-table-column prop="detail_id" label="明细号" min-width="160" />
      <el-table-column prop="contract_no" label="合同号" min-width="150" />
      <el-table-column prop="customer_name" label="客户名称" min-width="120" />
      <el-table-column prop="product_model" label="产品型号" min-width="120" />
      <el-table-column prop="air_permeability" label="透气量" width="90" />
      <el-table-column prop="req_length" label="需求长度(m)" width="110" />
      <el-table-column prop="req_width" label="需求宽度(m)" width="110" />
    </el-table>
    <template #footer>
      <el-button type="primary" @click="relatedDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const rows = ref([]);
const activeTab = ref("production");
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const detailDialogVisible = ref(false);
const detailData = ref({});
const relatedDialogVisible = ref(false);
const relatedLoading = ref(false);
const relatedBatchNo = ref("");
const relatedOrders = ref([]);

const pendingRows = computed(() => rows.value.filter((r) => Number(r.process_status) === 1));
const completedRows = computed(() => rows.value.filter((r) => Number(r.process_status) !== 1));
const pendingPager = reactive({ pageNo: 1, pageSize: 10 });
const completedPager = reactive({ pageNo: 1, pageSize: 10 });

const pendingPageRows = computed(() => {
  const start = (pendingPager.pageNo - 1) * pendingPager.pageSize;
  const end = start + pendingPager.pageSize;
  return pendingRows.value.slice(start, end);
});

const completedPageRows = computed(() => {
  const start = (completedPager.pageNo - 1) * completedPager.pageSize;
  const end = start + completedPager.pageSize;
  return completedRows.value.slice(start, end);
});

const query = reactive({ batchNo: "", processStatus: null });
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
const parsePage = (payload) => (payload?.records && Array.isArray(payload.records) ? { records: payload.records, total: Number(payload.total || 0) } : { records: [], total: 0 });
const getCurrentUserId = async () => {
  const res = await request.get("/api/auth/me");
  const data = parseData(res);
  return data?.userId || "";
};

const fetchRows = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/production/weaving-orders", {
      params: { pageNo: 1, pageSize: 2000, batchNo: query.batchNo || undefined, processStatus: query.processStatus ?? undefined }
    });
    const page = parsePage(parseData(res));
    rows.value = page.records;
    pendingPager.pageNo = 1;
    completedPager.pageNo = 1;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载织造订单失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.batchNo = "";
  query.processStatus = null;
  fetchRows();
};

const loadReportData = async (batchNo) => {
  const res = await request.get(`/api/production/weaving-orders/${batchNo}/report`);
  return parseData(res);
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
    if (!reportForm.operatorId) {
      reportForm.operatorId = await getCurrentUserId();
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载报工信息失败");
    return;
  }
  reportDialogVisible.value = true;
};

const openDetail = async (row) => {
  try {
    const report = await loadReportData(row.weaving_batch_no);
    detailData.value = { ...row, ...report };
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载报工详情失败");
  }
};

const openRelatedOrders = async (row) => {
  relatedBatchNo.value = row.weaving_batch_no;
  relatedLoading.value = true;
  try {
    const res = await request.get(`/api/production/weaving-orders/${row.weaving_batch_no}/related-orders`);
    const data = parseData(res);
    relatedOrders.value = Array.isArray(data) ? data : [];
    relatedDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载相关订单失败");
  } finally {
    relatedLoading.value = false;
  }
};

const submitReport = async () => {
  if (!reportForm.machineId) return ElMessage.warning("请填写机台号");
  let operatorId = String(reportForm.operatorId || "").trim();
  if (!operatorId) {
    try {
      operatorId = await getCurrentUserId();
      reportForm.operatorId = operatorId;
    } catch (_) {
      operatorId = "";
    }
  }
  if (!operatorId) return ElMessage.warning("请填写操作工号");
  if (!reportForm.materialBatchNo) return ElMessage.warning("请填写原料批次号");
  if (!reportForm.tensionParams) return ElMessage.warning("请填写张力参数");
  if (reportForm.actualLength === null || reportForm.actualLength === undefined) return ElMessage.warning("请填写实际产出长度");
  if (!reportForm.actualStartTime) return ElMessage.warning("请填写实际开工时间");

  reportSubmitting.value = true;
  try {
    await request.post(`/api/production/weaving-orders/${reportForm.weavingBatchNo}/report`, {
      machineId: reportForm.machineId,
      operatorId,
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
.section-pager { margin: 10px 0 14px; display: flex; justify-content: flex-end; }
.related-tip { margin-bottom: 10px; }
</style>
