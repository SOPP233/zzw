<template>
  <el-card>
    <template #header>
      <span>{{ title }}</span>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="批次号">
        <el-input v-model.trim="query.batchNo" placeholder="请输入批次号" clearable @keyup.enter="fetchRows" />
      </el-form-item>
      <el-form-item label="状态">
        <el-input-number v-model="query.processStatus" :min="1" :controls="false" placeholder="状态值" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchRows">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 130" />
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
  </el-card>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
  columns: { type: Array, default: () => [] }
});

const loading = ref(false);
const rows = ref([]);
const total = ref(0);

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  batchNo: "",
  processStatus: null
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
    const res = await request.get(props.endpoint, {
      params: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        batchNo: query.batchNo || undefined,
        processStatus: query.processStatus ?? undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载列表失败");
  } finally {
    loading.value = false;
  }
};

const onSizeChange = () => {
  query.pageNo = 1;
  fetchRows();
};

const resetQuery = () => {
  query.batchNo = "";
  query.processStatus = null;
  query.pageNo = 1;
  fetchRows();
};

fetchRows();
</script>

<style scoped>
.search-bar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
