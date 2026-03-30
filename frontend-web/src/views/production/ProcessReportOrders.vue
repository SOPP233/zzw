<template>
  <el-card>
    <template #header><span>{{ title }}</span></template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="批次号">
        <el-input v-model.trim="query.batchNo" placeholder="请输入批次号" clearable @keyup.enter="fetchRows" />
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
        <el-table v-loading="loading" :data="productionPageRows" border>
          <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 130" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openReport(row)">报工</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="productionPager.pageNo"
            v-model:page-size="productionPager.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="productionRows.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="完工区" name="completed">
        <el-table v-loading="loading" :data="completedPageRows" border>
          <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 130" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link @click="openDetail(row)">查看报工</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="completedPager.pageNo"
            v-model:page-size="completedPager.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="completedRows.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
    <el-form :model="form" label-width="130px">
      <el-form-item :label="batchLabel">
        <el-input :model-value="form.batchNo" disabled />
      </el-form-item>
      <el-form-item v-for="field in fields" :key="field.key" :label="field.label" :required="!!field.required">
        <el-input v-if="field.type === 'text'" v-model.trim="form[field.key]" />
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="form[field.key]"
          :min="field.min ?? 0"
          :precision="field.precision ?? 2"
          :controls="false"
          style="width: 100%"
        />
        <el-select v-else-if="field.type === 'select'" v-model="form[field.key]" style="width: 100%">
          <el-option v-for="op in field.options || []" :key="op.value" :label="op.label" :value="op.value" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitReport">提交报工</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailDialogVisible" title="报工详情" width="620px" destroy-on-close>
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="batchLabel">{{ detailData[batchProp] || "-" }}</el-descriptions-item>
      <el-descriptions-item v-for="field in fields" :key="`detail-${field.key}`" :label="field.label">
        {{ displayField(field) }}
      </el-descriptions-item>
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
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
  reportEndpointPrefix: { type: String, required: true },
  batchProp: { type: String, required: true },
  batchLabel: { type: String, default: "批次号" },
  dialogTitle: { type: String, default: "报工" },
  columns: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  doneStatus: { type: Number, default: 2 }
});

const loading = ref(false);
const rows = ref([]);
const dialogVisible = ref(false);
const detailDialogVisible = ref(false);
const submitting = ref(false);
const activeTab = ref("production");
const detailData = ref({});
const form = reactive({ batchNo: "" });
const query = reactive({ batchNo: "", processStatus: null });

const productionPager = reactive({ pageNo: 1, pageSize: 10 });
const completedPager = reactive({ pageNo: 1, pageSize: 10 });

const parseData = (payload) => payload?.data ?? payload ?? {};
const parsePage = (payload) =>
  payload?.records && Array.isArray(payload.records) ? { records: payload.records } : { records: [] };

const isDone = (row) => Number(row?.process_status) !== 1;
const productionRows = computed(() => rows.value.filter((r) => !isDone(r)));
const completedRows = computed(() => rows.value.filter((r) => isDone(r)));

const productionPageRows = computed(() => {
  const start = (productionPager.pageNo - 1) * productionPager.pageSize;
  return productionRows.value.slice(start, start + productionPager.pageSize);
});

const completedPageRows = computed(() => {
  const start = (completedPager.pageNo - 1) * completedPager.pageSize;
  return completedRows.value.slice(start, start + completedPager.pageSize);
});

const resetForm = () => {
  props.fields.forEach((f) => {
    form[f.key] = f.type === "number" ? null : "";
  });
};

const reportUrl = (batchNo) => `/api/production/${props.reportEndpointPrefix}/${batchNo}/report`;

const fetchRows = async () => {
  loading.value = true;
  try {
    const res = await request.get(props.endpoint, {
      params: { pageNo: 1, pageSize: 2000, batchNo: query.batchNo || undefined, processStatus: query.processStatus ?? undefined }
    });
    rows.value = parsePage(parseData(res)).records;
    productionPager.pageNo = 1;
    completedPager.pageNo = 1;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "加载列表失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.batchNo = "";
  query.processStatus = null;
  fetchRows();
};

const openReport = async (row) => {
  form.batchNo = row[props.batchProp];
  resetForm();
  try {
    const res = await request.get(reportUrl(form.batchNo));
    const report = parseData(res);
    props.fields.forEach((f) => {
      const key = f.apiKey || f.key;
      form[f.key] = report[key] ?? form[f.key];
    });
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "加载报工信息失败");
    return;
  }
  dialogVisible.value = true;
};

const openDetail = async (row) => {
  try {
    const res = await request.get(reportUrl(row[props.batchProp]));
    detailData.value = { ...row, ...parseData(res) };
    detailDialogVisible.value = true;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "加载报工详情失败");
  }
};

const displayField = (field) => {
  const key = field.apiKey || field.key;
  const value = detailData.value?.[key];
  if (value === null || value === undefined || value === "") return "-";
  if (field.type === "select") {
    const hit = (field.options || []).find((op) => String(op.value) === String(value));
    return hit ? hit.label : value;
  }
  return value;
};

const submitReport = async () => {
  for (const f of props.fields) {
    if (!f.required) continue;
    const v = form[f.key];
    if (v === null || v === undefined || (typeof v === "string" && !v.trim())) {
      ElMessage.warning(`请填写${f.label}`);
      return;
    }
  }

  const payload = {};
  props.fields.forEach((f) => {
    payload[f.key] = form[f.key];
  });

  submitting.value = true;
  try {
    await request.post(reportUrl(form.batchNo), payload);
    ElMessage.success("报工已提交");
    dialogVisible.value = false;
    fetchRows();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "提交报工失败");
  } finally {
    submitting.value = false;
  }
};

fetchRows();
</script>

<style scoped>
.search-bar { margin-bottom: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
