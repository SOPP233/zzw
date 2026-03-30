<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row"><span>生产审核</span></div>
      </template>

      <el-alert
        title="按织造订单审核：多张明细合并后的织造批次在此审核通过后，才会进入织造生产区。"
        type="info"
        :closable="false"
        show-icon
        class="mb-12"
      />

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="织造批号">
          <el-input v-model="query.batchNo" placeholder="请输入织造批号" clearable @keyup.enter="fetchRows" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchRows">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border row-key="weaving_batch_no">
        <el-table-column prop="weaving_batch_no" label="织造批号" min-width="170" />
        <el-table-column prop="machine_id" label="机台" width="100" />
        <el-table-column prop="actual_length" label="织造长度(m)" width="120" />
        <el-table-column prop="actual_width" label="织造宽度(m)" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link @click="approve(row)">审核通过并流转织造</el-button>
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
          @current-change="fetchRows"
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

const loading = ref(false);
const rows = ref([]);
const total = ref(0);

const query = reactive({
  batchNo: "",
  pageNo: 1,
  pageSize: 10
});

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) {
    return { records: payload.records, total: Number(payload.total || 0) };
  }
  if (Array.isArray(payload)) {
    return { records: payload, total: payload.length };
  }
  return { records: [], total: 0 };
};

const fetchRows = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/production/weaving-review-orders", {
      params: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        batchNo: query.batchNo || undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    ElMessage.error(error?.response?.data?.message || "加载织造审核列表失败");
  } finally {
    loading.value = false;
  }
};

const approve = async (row) => {
  await ElMessageBox.confirm(`确认审核通过织造批次 ${row.weaving_batch_no} 并流转织造生产区？`, "生产审核确认", {
    type: "warning"
  });
  try {
    await request.post(`/api/production/weaving-review-orders/${row.weaving_batch_no}/approve`);
    ElMessage.success("审核通过，织造批次已流转");
    fetchRows();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "审核提交失败");
  }
};

const resetQuery = () => {
  query.batchNo = "";
  query.pageNo = 1;
  fetchRows();
};

const onPageSizeChange = () => {
  query.pageNo = 1;
  fetchRows();
};

fetchRows();
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
