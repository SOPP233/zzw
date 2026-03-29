<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>排产工作区</span>
        <el-button type="primary" :disabled="selectedRows.length === 0" @click="openMergeDialog">合并生产</el-button>
      </div>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="订单号">
        <el-input v-model.trim="query.orderNo" placeholder="请输入订单号" clearable @keyup.enter="fetchRows" />
      </el-form-item>
      <el-form-item label="型号">
        <el-input v-model.trim="query.productModel" placeholder="请输入型号" clearable @keyup.enter="fetchRows" />
      </el-form-item>
      <el-form-item label="透气量">
        <el-input-number v-model="query.airPermeability" :min="0" :controls="false" placeholder="透气量" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchRows">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      title="支持单张网排产或多张网合并排产。仅允许型号与透气量完全一致的明细合并。"
      type="info"
      :closable="false"
      style="margin-bottom: 12px"
    />

    <el-table v-loading="loading" :data="rows" border @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="detailId" label="明细编号" min-width="150" />
      <el-table-column prop="orderNo" label="订单号" min-width="130" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="productModel" label="型号" min-width="130" />
      <el-table-column prop="airPermeability" label="透气量" width="100" />
      <el-table-column prop="reqLength" label="需求长度(m)" width="120" />
      <el-table-column prop="reqWidth" label="需求宽度(m)" width="120" />
      <el-table-column prop="expectedDate" label="交期" width="120" />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="fetchRows"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="合并生产" width="560px">
      <el-form label-width="110px">
        <el-form-item label="织造单号">
          <el-input v-model.trim="mergeForm.weavingBatchNo" placeholder="可手动填写，留空自动生成" />
        </el-form-item>
        <el-form-item label="织造长度(m)">
          <el-input-number v-model="mergeForm.weavingLength" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="织造宽度(m)">
          <el-input-number v-model="mergeForm.weavingWidth" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机台">
          <el-select v-model="mergeForm.machineId" placeholder="请选择机台" style="width: 100%">
            <el-option v-for="item in machineOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMerge">确认排产</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../utils/request";

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const rows = ref([]);
const total = ref(0);
const selectedRows = ref([]);

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: "",
  productModel: "",
  airPermeability: null
});

const mergeForm = reactive({
  weavingBatchNo: "",
  weavingLength: null,
  weavingWidth: null,
  machineId: ""
});

const machineOptions = Array.from({ length: 8 }).map((_, index) => {
  const no = `${index + 1}`.padStart(2, "0");
  return { label: `${index + 1}号机台`, value: `MC-${no}` };
});

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) {
    return { records: payload.records, total: Number(payload.total || 0) };
  }
  return { records: [], total: 0 };
};

const fetchRows = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/schedule/workbench", {
      params: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        orderNo: query.orderNo || undefined,
        productModel: query.productModel || undefined,
        airPermeability: query.airPermeability ?? undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载排产数据失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.orderNo = "";
  query.productModel = "";
  query.airPermeability = null;
  query.pageNo = 1;
  fetchRows();
};

const onSizeChange = () => {
  query.pageNo = 1;
  fetchRows();
};

const onSelectionChange = (selection) => {
  selectedRows.value = selection;
};

const openMergeDialog = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning("请至少选择一条明细");
    return;
  }
  const modelSet = new Set(selectedRows.value.map((item) => item.productModel));
  const airSet = new Set(selectedRows.value.map((item) => item.airPermeability));
  if (modelSet.size !== 1 || airSet.size !== 1) {
    ElMessage.warning("仅支持型号和透气量一致的明细合并排产");
    return;
  }
  dialogVisible.value = true;
};

const submitMerge = async () => {
  if (!mergeForm.machineId) {
    ElMessage.warning("请选择机台");
    return;
  }
  if (!mergeForm.weavingLength || mergeForm.weavingLength <= 0) {
    ElMessage.warning("请填写有效的织造长度");
    return;
  }
  if (!mergeForm.weavingWidth || mergeForm.weavingWidth <= 0) {
    ElMessage.warning("请填写有效的织造宽度");
    return;
  }

  submitting.value = true;
  try {
    await request.post("/api/schedule/merge", {
      detailIds: selectedRows.value.map((item) => item.detailId),
      weavingBatchNo: mergeForm.weavingBatchNo || undefined,
      weavingLength: mergeForm.weavingLength,
      weavingWidth: mergeForm.weavingWidth,
      machineId: mergeForm.machineId
    });
    ElMessage.success("排产成功，已进入织造订单");
    dialogVisible.value = false;
    mergeForm.weavingBatchNo = "";
    mergeForm.weavingLength = null;
    mergeForm.weavingWidth = null;
    mergeForm.machineId = "";
    selectedRows.value = [];
    fetchRows();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "排产失败");
  } finally {
    submitting.value = false;
  }
};

fetchRows();
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-bar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
