<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>设备与产能信息模型</span>
        </div>
      </template>

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="设备编号">
          <el-input v-model="query.assetCode" placeholder="请输入设备编号" clearable @keyup.enter="fetchEquipments" />
        </el-form-item>
        <el-form-item label="工段">
          <el-input v-model="query.workSection" placeholder="例如 织造段/定型段" clearable @keyup.enter="fetchEquipments" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchEquipments">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="equipments" border row-key="equipmentId">
        <el-table-column prop="equipmentId" label="设备ID" min-width="130" />
        <el-table-column prop="assetCode" label="固定资产编号" min-width="150" />
        <el-table-column prop="equipmentName" label="设备名称" min-width="130" />
        <el-table-column prop="workSection" label="所属工段" min-width="130" />
        <el-table-column prop="supportedProductRange" label="适用产品范围" min-width="180" />
        <el-table-column prop="capacityFactor" label="标准产能系数" min-width="120" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const equipments = ref([]);
const query = reactive({
  assetCode: "",
  workSection: ""
});

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const fetchEquipments = async () => {
  loading.value = true;
  try {
    // API 骨架：基础数据-设备与产能字典
    const res = await request.get("/api/basic/equipments", {
      params: {
        assetCode: query.assetCode || undefined,
        workSection: query.workSection || undefined
      }
    });
    equipments.value = normalizeRows(res?.data ?? res);
  } catch (error) {
    equipments.value = [];
    ElMessage.warning(error?.response?.status === 404 ? "设备字典接口待接入" : "加载设备字典失败");
  } finally {
    loading.value = false;
  }
};

fetchEquipments();
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

