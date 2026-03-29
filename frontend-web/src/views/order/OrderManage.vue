<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">订单管理（录入与分页）</div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="客户录入与分页" name="customers">
          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">客户录入表单</div></template>
            <el-form label-width="90px">
              <el-row :gutter="12">
                <el-col :span="12"><el-form-item label="客户ID"><el-input v-model.trim="customerForm.customerId" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="客户名称"><el-input v-model.trim="customerForm.customerName" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="联系人"><el-input v-model.trim="customerForm.contactName" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="联系电话"><el-input v-model.trim="customerForm.contactPhone" /></el-form-item></el-col>
                <el-col :span="12">
                  <el-form-item label="状态">
                    <el-select v-model="customerForm.status" style="width: 100%">
                      <el-option label="有效" :value="1" />
                      <el-option label="停用" :value="0" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
            <div class="action-row">
              <el-button @click="resetCustomerForm">重置</el-button>
              <el-button type="primary" :loading="customerSubmitLoading" @click="submitCustomer">提交客户</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">客户分页列表</div></template>
            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="客户名称"><el-input v-model="customerQuery.customerName" clearable @keyup.enter="fetchCustomers" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="customerLoading" @click="fetchCustomers">查询</el-button>
                <el-button @click="resetCustomerQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-table v-loading="customerLoading" :data="customers" border row-key="customerId">
              <el-table-column prop="customerId" label="客户ID" min-width="140" />
              <el-table-column prop="customerName" label="客户名称" min-width="140" />
              <el-table-column prop="contactName" label="联系人" min-width="120" />
              <el-table-column prop="contactPhone" label="联系电话" min-width="140" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '有效' : '停用' }}</el-tag></template>
              </el-table-column>
            </el-table>
            <div class="pager">
              <el-pagination
                v-model:current-page="customerQuery.pageNo"
                v-model:page-size="customerQuery.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="customerTotal"
                :page-sizes="[10, 20, 50]"
                @current-change="fetchCustomers"
                @size-change="onCustomerPageSizeChange"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="合同录入与分页" name="contracts">
          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">合同录入表单</div></template>
            <el-form label-width="90px">
              <el-row :gutter="12">
                <el-col :span="12"><el-form-item label="合同号"><el-input v-model.trim="contractForm.contractId" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="客户ID"><el-input v-model.trim="contractForm.customerId" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="签订日期"><el-date-picker v-model="contractForm.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
                <el-col :span="12"><el-form-item label="合同金额"><el-input-number v-model="contractForm.contractAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
                <el-col :span="12">
                  <el-form-item label="状态">
                    <el-select v-model="contractForm.contractStatus" style="width: 100%">
                      <el-option label="作废" :value="0" />
                      <el-option label="生效" :value="1" />
                      <el-option label="已完成" :value="2" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="交付地址"><el-input v-model.trim="contractForm.deliveryAddress" /></el-form-item>
              <el-form-item label="备注"><el-input v-model.trim="contractForm.remark" /></el-form-item>
            </el-form>
            <div class="action-row">
              <el-button @click="resetContractForm">重置</el-button>
              <el-button type="primary" :loading="contractSubmitLoading" @click="submitContract">提交合同</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">合同分页列表</div></template>
            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="合同号"><el-input v-model="contractQuery.contractId" clearable @keyup.enter="fetchContracts" /></el-form-item>
              <el-form-item label="客户ID"><el-input v-model="contractQuery.customerId" clearable @keyup.enter="fetchContracts" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="contractLoading" @click="fetchContracts">查询</el-button>
                <el-button @click="resetContractQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-table v-loading="contractLoading" :data="contracts" border row-key="contractId">
              <el-table-column prop="contractId" label="合同号" min-width="180" />
              <el-table-column prop="customerId" label="客户ID" min-width="120" />
              <el-table-column prop="contractAmount" label="合同金额" min-width="110" />
              <el-table-column prop="signDate" label="签订日期" min-width="120" />
              <el-table-column prop="deliveryAddress" label="交付地址" min-width="160" />
              <el-table-column prop="contractStatus" label="状态" width="100" />
            </el-table>
            <div class="pager">
              <el-pagination
                v-model:current-page="contractQuery.pageNo"
                v-model:page-size="contractQuery.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="contractTotal"
                :page-sizes="[10, 20, 50]"
                @current-change="fetchContracts"
                @size-change="onContractPageSizeChange"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="明细订单录入与分页" name="orders">
          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">订单录入表单</div></template>
            <el-form label-width="92px">
              <el-row :gutter="12">
                <el-col :span="8"><el-form-item label="订单号"><el-input v-model.trim="orderForm.orderNo" placeholder="留空自动生成" /></el-form-item></el-col>
                <el-col :span="8">
                  <el-form-item label="合同号">
                    <el-select v-model="orderForm.contractId" filterable placeholder="请选择合同号" style="width: 100%">
                      <el-option v-for="item in contracts" :key="item.contractId" :label="`${item.contractId} / ${item.customerId}`" :value="item.contractId" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8"><el-form-item label="交期"><el-date-picker v-model="orderForm.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
              </el-row>
            </el-form>

            <div class="detail-head">
              <span>订单明细</span>
              <el-button size="small" @click="addDetailRow">新增明细</el-button>
            </div>
            <div v-for="(item, idx) in orderForm.details" :key="idx" class="detail-row">
              <el-row :gutter="8">
                <el-col :span="5"><el-input v-model.trim="item.productModel" placeholder="型号" /></el-col>
                <el-col :span="5"><el-input-number v-model="item.airPermeability" :min="0" :controls="false" placeholder="透气量" style="width:100%" /></el-col>
                <el-col :span="5"><el-input-number v-model="item.reqLength" :min="0" :precision="2" :controls="false" placeholder="长度(m)" style="width:100%" /></el-col>
                <el-col :span="5"><el-input-number v-model="item.reqWidth" :min="0" :precision="2" :controls="false" placeholder="宽度(m)" style="width:100%" /></el-col>
                <el-col :span="4" class="detail-actions"><el-button type="danger" link @click="removeDetailRow(idx)" :disabled="orderForm.details.length === 1">删除</el-button></el-col>
              </el-row>
            </div>

            <div class="action-row">
              <el-button @click="resetOrderForm">重置</el-button>
              <el-button type="primary" :loading="orderSubmitLoading" @click="submitOrder">提交订单</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">订单分页列表</div></template>
            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="订单号"><el-input v-model="orderQuery.orderId" clearable @keyup.enter="fetchOrders" /></el-form-item>
              <el-form-item label="客户"><el-input v-model="orderQuery.customerName" clearable @keyup.enter="fetchOrders" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="orderLoading" @click="fetchOrders">查询</el-button>
                <el-button @click="resetOrderQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-table v-loading="orderLoading" :data="orders" border row-key="orderId">
              <el-table-column type="expand" width="56">
                <template #default="{ row }">
                  <el-table :data="row.details || []" size="small" border>
                    <el-table-column prop="detailId" label="明细编号" min-width="160" />
                    <el-table-column prop="productModel" label="产品型号" min-width="130" />
                    <el-table-column prop="airPermeability" label="透气量" width="100" />
                    <el-table-column prop="reqLength" label="长度(m)" width="110" />
                    <el-table-column prop="reqWidth" label="宽度(m)" width="110" />
                  </el-table>
                </template>
              </el-table-column>
              <el-table-column prop="orderNo" label="订单号" min-width="170" />
              <el-table-column prop="customerName" label="客户" min-width="130" />
              <el-table-column prop="contractNo" label="合同号" min-width="170" />
              <el-table-column prop="expectedDate" label="交期" width="120" />
            </el-table>
            <div class="pager">
              <el-pagination
                v-model:current-page="orderQuery.pageNo"
                v-model:page-size="orderQuery.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="orderTotal"
                :page-sizes="[10, 20, 50]"
                @current-change="fetchOrders"
                @size-change="onOrderPageSizeChange"
              />
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const activeTab = ref("customers");

const orderLoading = ref(false);
const customerLoading = ref(false);
const contractLoading = ref(false);
const orderSubmitLoading = ref(false);
const customerSubmitLoading = ref(false);
const contractSubmitLoading = ref(false);

const orders = ref([]);
const customers = ref([]);
const contracts = ref([]);

const orderTotal = ref(0);
const customerTotal = ref(0);
const contractTotal = ref(0);

const orderQuery = reactive({ orderId: "", customerName: "", pageNo: 1, pageSize: 10 });
const customerQuery = reactive({ customerName: "", pageNo: 1, pageSize: 10 });
const contractQuery = reactive({ contractId: "", customerId: "", pageNo: 1, pageSize: 10 });

const createDefaultDetail = () => ({ productModel: "", airPermeability: null, reqLength: null, reqWidth: null });

const orderForm = reactive({
  orderNo: "",
  contractId: "",
  expectedDate: "",
  orderStatus: 1,
  details: [createDefaultDetail()]
});

const customerForm = reactive({ customerId: "", customerName: "", contactName: "", contactPhone: "", status: 1 });

const contractForm = reactive({
  contractId: "",
  customerId: "",
  contractAmount: 0,
  signDate: "",
  deliveryAddress: "",
  contractStatus: 1,
  remark: ""
});

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) return { records: payload.records, total: Number(payload.total || 0) };
  if (Array.isArray(payload)) return { records: payload, total: payload.length };
  return { records: [], total: 0 };
};

const fetchOrders = async () => {
  orderLoading.value = true;
  try {
    const res = await request.get("/api/orders/full", {
      params: {
        pageNo: orderQuery.pageNo,
        pageSize: orderQuery.pageSize,
        orderId: orderQuery.orderId || undefined,
        customerName: orderQuery.customerName || undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    orders.value = page.records;
    orderTotal.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载订单失败");
  } finally {
    orderLoading.value = false;
  }
};

const fetchCustomers = async () => {
  customerLoading.value = true;
  try {
    const res = await request.get("/api/customers", { params: customerQuery });
    const page = parsePage(res?.data ?? res);
    customers.value = page.records;
    customerTotal.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载客户失败");
  } finally {
    customerLoading.value = false;
  }
};

const fetchContracts = async () => {
  contractLoading.value = true;
  try {
    const res = await request.get("/api/contracts", { params: contractQuery });
    const page = parsePage(res?.data ?? res);
    contracts.value = page.records;
    contractTotal.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载合同失败");
  } finally {
    contractLoading.value = false;
  }
};

const resetOrderForm = () => {
  orderForm.orderNo = "";
  orderForm.contractId = "";
  orderForm.expectedDate = "";
  orderForm.orderStatus = 1;
  orderForm.details = [createDefaultDetail()];
};

const resetCustomerForm = () => {
  customerForm.customerId = "";
  customerForm.customerName = "";
  customerForm.contactName = "";
  customerForm.contactPhone = "";
  customerForm.status = 1;
};

const resetContractForm = () => {
  contractForm.contractId = "";
  contractForm.customerId = "";
  contractForm.contractAmount = 0;
  contractForm.signDate = "";
  contractForm.deliveryAddress = "";
  contractForm.contractStatus = 1;
  contractForm.remark = "";
};

const resetOrderQuery = () => {
  orderQuery.orderId = "";
  orderQuery.customerName = "";
  orderQuery.pageNo = 1;
  fetchOrders();
};

const resetCustomerQuery = () => {
  customerQuery.customerName = "";
  customerQuery.pageNo = 1;
  fetchCustomers();
};

const resetContractQuery = () => {
  contractQuery.contractId = "";
  contractQuery.customerId = "";
  contractQuery.pageNo = 1;
  fetchContracts();
};

const onOrderPageSizeChange = () => {
  orderQuery.pageNo = 1;
  fetchOrders();
};

const onCustomerPageSizeChange = () => {
  customerQuery.pageNo = 1;
  fetchCustomers();
};

const onContractPageSizeChange = () => {
  contractQuery.pageNo = 1;
  fetchContracts();
};

const addDetailRow = () => orderForm.details.push(createDefaultDetail());
const removeDetailRow = (index) => {
  if (orderForm.details.length <= 1) return;
  orderForm.details.splice(index, 1);
};

const submitOrder = async () => {
  if (!orderForm.contractId) {
    ElMessage.warning("请选择合同号");
    return;
  }

  const validDetails = orderForm.details.filter((item) => item.productModel);
  if (validDetails.length === 0) {
    ElMessage.warning("至少填写一条有效明细");
    return;
  }

  if (validDetails.some((item) => item.airPermeability === null || item.reqLength === null || item.reqWidth === null)) {
    ElMessage.warning("明细的透气量、长度、宽度必须填写");
    return;
  }

  orderSubmitLoading.value = true;
  try {
    await request.post("/api/orders/full", {
      orderNo: orderForm.orderNo || undefined,
      contractId: orderForm.contractId,
      expectedDate: orderForm.expectedDate || undefined,
      orderStatus: orderForm.orderStatus,
      details: validDetails
    });
    ElMessage.success("订单提交成功");
    resetOrderForm();
    fetchOrders();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "订单提交失败");
  } finally {
    orderSubmitLoading.value = false;
  }
};

const submitCustomer = async () => {
  if (!customerForm.customerId || !customerForm.customerName) {
    ElMessage.warning("请填写客户ID和客户名称");
    return;
  }
  customerSubmitLoading.value = true;
  try {
    await request.post("/api/customers", customerForm);
    ElMessage.success("客户提交成功");
    resetCustomerForm();
    fetchCustomers();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "客户提交失败");
  } finally {
    customerSubmitLoading.value = false;
  }
};

const submitContract = async () => {
  if (!contractForm.contractId || !contractForm.customerId || !contractForm.signDate) {
    ElMessage.warning("请填写合同号、客户ID和签订日期");
    return;
  }
  contractSubmitLoading.value = true;
  try {
    await request.post("/api/contracts", contractForm);
    ElMessage.success("合同提交成功");
    resetContractForm();
    fetchContracts();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "合同提交失败");
  } finally {
    contractSubmitLoading.value = false;
  }
};

fetchOrders();
fetchCustomers();
fetchContracts();
</script>

<style scoped>
.page {
  padding: 4px;
}

.header-row {
  font-size: 16px;
  font-weight: 600;
}

.section-card {
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
}

.search-bar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
}

.detail-row {
  margin-bottom: 8px;
}

.detail-actions {
  display: flex;
  align-items: center;
}

.action-row {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>