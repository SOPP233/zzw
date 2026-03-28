<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>工序任务中心</span>
        </div>
      </template>

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="批次号">
          <el-input v-model="query.batchId" placeholder="输入批次号" clearable @keyup.enter="fetchTasks" />
        </el-form-item>
        <el-form-item label="工序类型">
          <el-select v-model="query.processType" clearable placeholder="全部工序" style="width: 140px">
            <el-option v-for="(label, key) in PROCESS_TYPE_MAP" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchTasks">刷新任务</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tasks" border>
        <el-table-column prop="taskId" label="任务ID" min-width="190" />
        <el-table-column prop="batchId" label="批次号" min-width="150" />
        <el-table-column label="工序" min-width="120">
          <template #default="{ row }">{{ PROCESS_TYPE_MAP[row.processType] || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ TASK_STATUS_MAP[row.status] || "-" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorId" label="操作人" min-width="110" />
        <el-table-column prop="endTime" label="完工时间" min-width="160" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="row.status === 3" @click="openCompleteDialog(row)">报工完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="工序报工" width="520px">
      <el-form label-width="120px">
        <el-form-item label="任务ID">
          <el-input :model-value="currentTask.taskId" disabled />
        </el-form-item>
        <el-form-item label="工序">
          <el-input :model-value="PROCESS_TYPE_MAP[currentTask.processType] || '-'" disabled />
        </el-form-item>
        <el-form-item label="操作员工号" required>
          <el-input v-model="form.operatorId" placeholder="请输入员工工号" />
        </el-form-item>

        <template v-if="currentTask.processType === 1">
          <el-form-item label="经纬密度参数">
            <el-input v-model="form.warpWeftDensity" />
          </el-form-item>
          <el-form-item label="送经轴张力参数">
            <el-input v-model="form.tensionParam" />
          </el-form-item>
          <el-form-item label="运行机台号">
            <el-input v-model="form.machineNo" />
          </el-form-item>
          <el-form-item label="投料批次号">
            <el-input v-model="form.materialBatchNo" />
          </el-form-item>
          <el-form-item label="实际产出米数">
            <el-input-number v-model="form.actualOutputMeters" :min="0" :precision="2" />
          </el-form-item>
        </template>

        <template v-if="currentTask.processType === 2">
          <el-form-item label="加热温度曲线">
            <el-input v-model="form.temperatureCurve" />
          </el-form-item>
          <el-form-item label="定型时长(分钟)">
            <el-input-number v-model="form.shapingDuration" :min="0" />
          </el-form-item>
          <el-form-item label="网面收缩率">
            <el-input-number v-model="form.shrinkageRate" :min="0" :max="1" :step="0.001" :precision="3" />
          </el-form-item>
        </template>

        <template v-if="currentTask.processType === 3">
          <el-form-item label="裁切长度">
            <el-input-number v-model="form.cutLength" :min="0" />
          </el-form-item>
          <el-form-item label="裁切宽度">
            <el-input-number v-model="form.cutWidth" :min="0" />
          </el-form-item>
          <el-form-item label="损耗面积">
            <el-input-number v-model="form.lossArea" :min="0" :precision="2" />
          </el-form-item>
        </template>

        <template v-if="currentTask.processType === 4">
          <el-form-item label="接头类型">
            <el-input v-model="form.jointType" />
          </el-form-item>
          <el-form-item label="接头强度标识">
            <el-input v-model="form.jointStrengthFlag" />
          </el-form-item>
        </template>

        <template v-if="currentTask.processType === 5">
          <el-form-item label="最终成型尺寸">
            <el-input v-model="form.finalSize" />
          </el-form-item>
          <el-form-item label="网病参数">
            <el-input v-model="form.defectParam" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitComplete">提交报工</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { PROCESS_TYPE_MAP, TASK_STATUS_MAP } from "../../constants/production";

const loading = ref(false);
const submitting = ref(false);
const tasks = ref([]);
const dialogVisible = ref(false);
const currentTask = ref({});

const query = reactive({
  batchId: "",
  processType: undefined
});

const form = reactive({
  operatorId: "",
  warpWeftDensity: "",
  tensionParam: "",
  machineNo: "",
  materialBatchNo: "",
  actualOutputMeters: undefined,
  temperatureCurve: "",
  shapingDuration: undefined,
  shrinkageRate: undefined,
  cutLength: undefined,
  cutWidth: undefined,
  lossArea: undefined,
  jointType: "",
  jointStrengthFlag: "",
  finalSize: "",
  defectParam: ""
});

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const fetchTasks = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/process-tasks", { params: { pageNo: 1, pageSize: 500 } });
    let rows = normalizeRows(res?.data ?? res);
    if (query.batchId) rows = rows.filter((item) => item.batchId?.includes(query.batchId));
    if (query.processType !== undefined) rows = rows.filter((item) => item.processType === query.processType);
    tasks.value = rows;
  } catch (error) {
    tasks.value = [];
    ElMessage.error(error?.response?.data?.message || "加载工序任务失败");
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.operatorId = "";
  form.warpWeftDensity = "";
  form.tensionParam = "";
  form.machineNo = "";
  form.materialBatchNo = "";
  form.actualOutputMeters = undefined;
  form.temperatureCurve = "";
  form.shapingDuration = undefined;
  form.shrinkageRate = undefined;
  form.cutLength = undefined;
  form.cutWidth = undefined;
  form.lossArea = undefined;
  form.jointType = "";
  form.jointStrengthFlag = "";
  form.finalSize = "";
  form.defectParam = "";
};

const openCompleteDialog = (row) => {
  currentTask.value = row;
  resetForm();
  dialogVisible.value = true;
};

const buildOutputData = () => {
  const base = {};
  if (currentTask.value.processType === 1) {
    base.warpWeftDensity = form.warpWeftDensity;
    base.tensionParam = form.tensionParam;
    base.machineNo = form.machineNo;
    base.materialBatchNo = form.materialBatchNo;
    base.actualOutputMeters = form.actualOutputMeters;
  }
  if (currentTask.value.processType === 2) {
    base.temperatureCurve = form.temperatureCurve;
    base.shapingDuration = form.shapingDuration;
    base.shrinkageRate = form.shrinkageRate;
  }
  if (currentTask.value.processType === 3) {
    base.cutLength = form.cutLength;
    base.cutWidth = form.cutWidth;
    base.lossArea = form.lossArea;
  }
  if (currentTask.value.processType === 4) {
    base.jointType = form.jointType;
    base.jointStrengthFlag = form.jointStrengthFlag;
  }
  if (currentTask.value.processType === 5) {
    base.finalSize = form.finalSize;
    base.defectParam = form.defectParam;
  }
  return base;
};

const submitComplete = async () => {
  if (!form.operatorId) {
    ElMessage.warning("请填写操作员工号");
    return;
  }
  submitting.value = true;
  try {
    const res = await request.post(`/api/tasks/${currentTask.value.taskId}/complete`, {
      operatorId: form.operatorId,
      outputData: buildOutputData()
    });
    const data = res?.data ?? res;
    ElMessage.success(`报工成功，下一任务：${data.nextTaskId || "无"}`);
    dialogVisible.value = false;
    fetchTasks();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "提交报工失败");
  } finally {
    submitting.value = false;
  }
};

fetchTasks();
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
</style>

