<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>产品清单与工艺路线配置</span>
        </div>
      </template>

      <el-form :inline="true" class="mb-12" @submit.prevent>
        <el-form-item label="标准型号">
          <el-input v-model="query.productModel" placeholder="请输入型号关键字" clearable @keyup.enter="fetchProducts" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchProducts">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="products" border row-key="productModel">
        <el-table-column prop="productModel" label="标准型号" min-width="150" />
        <el-table-column prop="materialTemplateCode" label="物料清单模板" min-width="150" />
        <el-table-column label="标准工艺路线" min-width="260">
          <template #default="{ row }">
            <el-tag
              v-for="(proc, index) in row.processRoute || []"
              :key="`${row.productModel}-${index}`"
              size="small"
              class="mr-6"
            >
              {{ proc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const products = ref([]);
const query = reactive({
  productModel: ""
});

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const fetchProducts = async () => {
  loading.value = true;
  try {
    // API 骨架：基础数据-产品与工艺路线字典
    const res = await request.get("/api/basic/products", {
      params: { productModel: query.productModel || undefined }
    });
    products.value = normalizeRows(res?.data ?? res);
  } catch (error) {
    products.value = [];
    ElMessage.warning(error?.response?.status === 404 ? "产品字典接口待接入" : "加载产品字典失败");
  } finally {
    loading.value = false;
  }
};

fetchProducts();
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

.mr-6 {
  margin-right: 6px;
}
</style>

