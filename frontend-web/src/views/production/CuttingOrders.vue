<template>
  <el-card>
    <template #header>
      <span>裁网订单</span>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="任务/批号">
        <el-input v-model.trim="query.batchNo" placeholder="请输入任务单号/定型批号" clearable @keyup.enter="fetchData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="生产区" name="production">
        <el-table v-loading="loading" :data="productionPageRows" border>
          <el-table-column prop="task_id" label="裁网任务单号" min-width="170" />
          <el-table-column prop="setting_batch_no" label="定型批号" min-width="170" />
          <el-table-column prop="source_length" label="大网长度(m)" width="120" />
          <el-table-column prop="source_width" label="大网宽度(m)" width="120" />
          <el-table-column prop="small_net_count" label="对应小网数" width="110" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openReport(row)">报工</el-button>
              <el-button type="success" link @click="openRelatedOrders(row.task_id)">查看对应小网</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="section-pager">
          <el-pagination
            v-model:current-page="productionPager.pageNo"
            v-model:page-size="productionPager.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="productionRows.length"
            :page-sizes="[5, 10, 20, 50]"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="完工区" name="completed">
        <el-table v-loading="loading" :data="completedPageRows" border>
          <el-table-column prop="cut_batch_no" label="小网批次号" min-width="170" />
          <el-table-column prop="task_id" label="裁网任务单号" min-width="170" />
          <el-table-column prop="detail_id" label="明细ID" min-width="160" />
          <el-table-column prop="actual_cut_len" label="裁切长度(m)" width="120" />
          <el-table-column prop="actual_cut_wid" label="裁切宽度(m)" width="120" />
          <el-table-column prop="operator_id" label="操作工号" width="140" />
          <el-table-column prop="cut_time" label="报工时间" min-width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link @click="openRelatedOrders(row.task_id)">查看对应小网</el-button>
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

  <el-dialog v-model="reportDialogVisible" title="裁网报工（大网完工）" width="520px" destroy-on-close>
    <el-form :model="reportForm" label-width="120px">
      <el-form-item label="裁网任务单号">
        <el-input :model-value="reportForm.taskId" disabled />
      </el-form-item>
      <el-form-item label="操作工号" required>
        <el-input v-model.trim="reportForm.operatorId" />
      </el-form-item>
      <el-form-item label="总损耗面积" required>
        <el-input-number v-model="reportForm.wasteArea" :min="0" :precision="2" :controls="false" style="width: 100%" />
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

  <el-dialog v-model="relatedDialogVisible" title="大网对应小网" width="980px" destroy-on-close>
    <el-alert :title="`裁网任务单号：${relatedTaskId || '-'}`" type="info" :closable="false" class="related-tip" />
    <el-table v-loading="relatedLoading" :data="relatedRows" border>
      <el-table-column prop="cut_batch_no" label="小网批次号" min-width="160" />
      <el-table-column prop="detail_id" label="明细号" min-width="160" />
      <el-table-column prop="order_no" label="订单号" min-width="160" />
      <el-table-column prop="contract_no" label="合同号" min-width="150" />
      <el-table-column prop="customer_name" label="客户名称" min-width="120" />
      <el-table-column prop="product_model" label="产品型号" min-width="120" />
      <el-table-column prop="req_length" label="需求长度(m)" width="110" />
      <el-table-column prop="req_width" label="需求宽度(m)" width="110" />
      <el-table-column prop="actual_cut_len" label="裁切长度(m)" width="110" />
      <el-table-column prop="actual_cut_wid" label="裁切宽度(m)" width="110" />
      <el-table-column prop="process_status" label="状态" width="90" />
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
const activeTab = ref("production");
const tasks = ref([]);
const records = ref([]);

const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = reactive({ taskId: "", operatorId: "", wasteArea: null });

const relatedDialogVisible = ref(false);
const relatedLoading = ref(false);
const relatedTaskId = ref("");
const relatedRows = ref([]);

const query = reactive({ batchNo: "" });

const productionRows = computed(() => tasks.value.filter((r) => Number(r.task_status) !== 3));
const completedRows = computed(() => records.value.filter((r) => Number(r.process_status) === 2));

const productionPager = reactive({ pageNo: 1, pageSize: 10 });
const completedPager = reactive({ pageNo: 1, pageSize: 10 });

const productionPageRows = computed(() => {
  const start = (productionPager.pageNo - 1) * productionPager.pageSize;
  return productionRows.value.slice(start, start + productionPager.pageSize);
});

const completedPageRows = computed(() => {
  const start = (completedPager.pageNo - 1) * completedPager.pageSize;
  return completedRows.value.slice(start, start + completedPager.pageSize);
});

const parseData = (payload) => payload?.data ?? payload ?? {};
const parsePage = (payload) => (payload?.records && Array.isArray(payload.records) ? payload : { records: [], total: 0 });

const getCurrentUserId = async () => {
  const res = await request.get("/api/auth/me");
  const data = parseData(res);
  return data?.userId || "";
};

const fetchTasks = async () => {
  const res = await request.get("/api/production/cutting-tasks", {
    params: { pageNo: 1, pageSize: 2000, batchNo: query.batchNo || undefined }
  });
  const page = parsePage(parseData(res));
  tasks.value = page.records || [];
};

const fetchRecords = async () => {
  const res = await request.get("/api/production/cutting-orders", {
    params: { pageNo: 1, pageSize: 2000, batchNo: query.batchNo || undefined }
  });
  const page = parsePage(parseData(res));
  records.value = page.records || [];
};

const fetchData = async () => {
  loading.value = true;
  try {
    await Promise.all([fetchTasks(), fetchRecords()]);
    productionPager.pageNo = 1;
    completedPager.pageNo = 1;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载裁网数据失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.batchNo = "";
  fetchData();
};

const openReport = async (row) => {
  reportForm.taskId = row.task_id;
  reportForm.wasteArea = null;
  reportForm.operatorId = "";
  try {
    reportForm.operatorId = await getCurrentUserId();
  } catch (_) {
    reportForm.operatorId = "";
  }
  reportDialogVisible.value = true;
};

const submitReport = async () => {
  if (!reportForm.taskId) return ElMessage.warning("任务单号不能为空");
  if (!reportForm.operatorId) return ElMessage.warning("请填写操作工号");
  if (reportForm.wasteArea === null || reportForm.wasteArea === undefined) return ElMessage.warning("请填写总损耗面积");

  reportSubmitting.value = true;
  try {
    await request.post(`/api/production/cutting-tasks/${reportForm.taskId}/complete`, {
      operatorId: reportForm.operatorId,
      wasteArea: reportForm.wasteArea
    });
    ElMessage.success("裁网报工已提交，已生成对应小网");
    reportDialogVisible.value = false;
    await fetchData();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "提交报工失败");
  } finally {
    reportSubmitting.value = false;
  }
};

const openRelatedOrders = async (taskId) => {
  relatedTaskId.value = taskId;
  relatedLoading.value = true;
  try {
    const res = await request.get(`/api/production/cutting-tasks/${taskId}/related-orders`);
    const data = parseData(res);
    relatedRows.value = Array.isArray(data) ? data : [];
    relatedDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载对应小网失败");
  } finally {
    relatedLoading.value = false;
  }
};

fetchData();
</script>

<style scoped>
.search-bar { margin-bottom: 12px; }
.section-pager { margin: 10px 0 14px; display: flex; justify-content: flex-end; }
.related-tip { margin-bottom: 10px; }
</style>
