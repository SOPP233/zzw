<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>订单管理（录入与分页）</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="订单录入与分页" name="orders">
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">订单录入表单</div>
            </template>
            <el-form label-width="92px">
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="订单号">
                    <el-input v-model.trim="orderForm.orderId" placeholder="留空自动生成" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="合同号">
                    <el-select v-model="orderForm.contractId" filterable placeholder="请选择合同号" style="width: 100%" @change="handleContractChange">
                      <el-option
                        v-for="item in contracts"
                        :key="item.contractId"
                        :label="`${item.contractId} / ${item.customerId}`"
                        :value="item.contractId"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="客户ID">
                    <el-input v-model.trim="orderForm.customerId" readonly placeholder="由合同自动带出" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="交期">
                    <el-date-picker v-model="orderForm.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="合同金额">
                    <el-input-number v-model="orderForm.totalAmount" :min="0" :precision="2" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="订单状态">
                    <el-select v-model="orderForm.orderStatus" style="width: 100%">
                      <el-option v-for="opt in ORDER_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="交付地址">
                <el-input v-model.trim="orderForm.deliveryAddress" placeholder="请输入交付地址" />
              </el-form-item>
            </el-form>

            <div class="detail-head">
              <span>订单明细</span>
              <el-button size="small" @click="addDetailRow">新增明细</el-button>
            </div>
            <div v-for="(item, idx) in orderForm.details" :key="idx" class="detail-row">
              <el-row :gutter="8">
                <el-col :span="4"><el-input v-model.trim="item.productModel" placeholder="型号" /></el-col>
                <el-col :span="4"><el-input-number v-model="item.airPermeability" :min="0" :controls="false" placeholder="透气量" /></el-col>
                <el-col :span="3"><el-input-number v-model="item.lengthReq" :min="0" :controls="false" placeholder="长度" /></el-col>
                <el-col :span="3"><el-input-number v-model="item.widthReq" :min="0" :controls="false" placeholder="宽度" /></el-col>
                <el-col :span="6"><el-input v-model.trim="item.craftReq" placeholder="工艺要求" /></el-col>
                <el-col :span="4" class="detail-actions">
                  <el-button type="danger" link @click="removeDetailRow(idx)" :disabled="orderForm.details.length === 1">删除</el-button>
                </el-col>
              </el-row>
            </div>

            <div class="action-row">
              <el-button @click="resetOrderForm">重置表单</el-button>
              <el-button type="primary" :loading="orderSubmitLoading" @click="submitOrder">提交订单</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">订单分页列表</div>
            </template>

            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="订单号">
                <el-input v-model="orderQuery.orderId" placeholder="请输入订单号" clearable @keyup.enter="fetchOrders" />
              </el-form-item>
              <el-form-item label="客户">
                <el-input v-model="orderQuery.customerName" placeholder="请输入客户名" clearable @keyup.enter="fetchOrders" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="orderQuery.orderStatus" placeholder="全部状态" clearable style="width: 160px">
                  <el-option v-for="opt in ORDER_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="orderLoading" @click="fetchOrders">查询</el-button>
                <el-button @click="resetOrderQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="orderLoading" :data="orders" border row-key="orderId">
              <el-table-column type="expand" width="56">
                <template #default="{ row }">
                  <div class="expand-wrap">
                    <el-table :data="row.details || []" size="small" border>
                      <el-table-column prop="detailId" label="明细编号" min-width="160" />
                      <el-table-column prop="productModel" label="产品型号" min-width="130" />
                      <el-table-column prop="airPermeability" label="透气量" min-width="100" />
                      <el-table-column prop="lengthReq" label="长度" width="90" />
                      <el-table-column prop="widthReq" label="宽度" width="90" />
                      <el-table-column prop="craftReq" label="工艺要求" min-width="150" />
                      <el-table-column label="明细状态" min-width="120">
                        <template #default="{ row: detail }">
                          <el-tag size="small">{{ detailStatusText(detail.detailStatus) }}</el-tag>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="orderId" label="订单号" min-width="180" />
              <el-table-column prop="customerName" label="客户" min-width="130" />
              <el-table-column prop="signDate" label="签订日期" min-width="120" />
              <el-table-column prop="totalAmount" label="合同总金额" min-width="120" />
              <el-table-column prop="deliveryAddress" label="交付地址" min-width="180" />
              <el-table-column label="订单状态" min-width="120">
                <template #default="{ row }">
                  <el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openLogDrawer(row)">流转日志</el-button>
                </template>
              </el-table-column>
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

        <el-tab-pane label="合同录入与分页" name="contracts">
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">合同录入表单</div>
            </template>
            <el-form label-width="90px">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="合同号">
                    <el-input v-model.trim="contractForm.contractId" placeholder="例如 CON20260328001" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="客户ID">
                    <el-input v-model.trim="contractForm.customerId" placeholder="例如 CUS001" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="签订日期">
                    <el-date-picker v-model="contractForm.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="合同金额">
                    <el-input-number v-model="contractForm.contractAmount" :min="0" :precision="2" style="width: 100%" />
                  </el-form-item>
                </el-col>
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
              <el-form-item label="交付地址">
                <el-input v-model.trim="contractForm.deliveryAddress" placeholder="请输入交付地址" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model.trim="contractForm.remark" placeholder="请输入备注" />
              </el-form-item>
            </el-form>
            <div class="action-row">
              <el-button @click="resetContractForm">重置表单</el-button>
              <el-button type="primary" :loading="contractSubmitLoading" @click="submitContract">提交合同</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">合同分页列表</div>
            </template>
            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="合同号">
                <el-input v-model="contractQuery.contractId" placeholder="请输入合同号" clearable @keyup.enter="fetchContracts" />
              </el-form-item>
              <el-form-item label="客户ID">
                <el-input v-model="contractQuery.customerId" placeholder="请输入客户ID" clearable @keyup.enter="fetchContracts" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="contractLoading" @click="fetchContracts">查询</el-button>
                <el-button @click="resetContractQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-table v-loading="contractLoading" :data="contracts" border row-key="contractId">
              <el-table-column prop="contractId" label="合同号" min-width="180" />
              <el-table-column prop="customerId" label="客户ID" min-width="140" />
              <el-table-column prop="contractAmount" label="合同金额" min-width="120" />
              <el-table-column prop="signDate" label="签订日期" min-width="120" />
              <el-table-column prop="deliveryAddress" label="交付地址" min-width="180" />
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.contractStatus === 0 ? "作废" : row.contractStatus === 2 ? "已完成" : "生效" }}</el-tag>
                </template>
              </el-table-column>
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

        <el-tab-pane label="客户录入与分页" name="customers">
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">客户录入表单</div>
            </template>

            <el-form label-width="90px">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="客户ID">
                    <el-input v-model.trim="customerForm.customerId" placeholder="例如 CUS002" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="客户名称">
                    <el-input v-model.trim="customerForm.customerName" placeholder="请输入客户名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系人">
                    <el-input v-model.trim="customerForm.contactName" placeholder="请输入联系人" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话">
                    <el-input v-model.trim="customerForm.contactPhone" placeholder="请输入联系电话" />
                  </el-form-item>
                </el-col>
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
              <el-button @click="resetCustomerForm">重置表单</el-button>
              <el-button type="primary" :loading="customerSubmitLoading" @click="submitCustomer">提交客户</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-title">客户分页列表</div>
            </template>
            <el-form :inline="true" class="search-bar" @submit.prevent>
              <el-form-item label="客户名称">
                <el-input v-model="customerQuery.customerName" placeholder="请输入客户名称" clearable @keyup.enter="fetchCustomers" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="customerLoading" @click="fetchCustomers">查询</el-button>
                <el-button @click="resetCustomerQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="customerLoading" :data="customers" border row-key="customerId">
              <el-table-column prop="customerId" label="客户ID" min-width="160" />
              <el-table-column prop="customerName" label="客户名称" min-width="150" />
              <el-table-column prop="contactName" label="联系人" min-width="120" />
              <el-table-column prop="contactPhone" label="联系电话" min-width="150" />
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "有效" : "停用" }}</el-tag>
                </template>
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
      </el-tabs>
    </el-card>

    <el-drawer v-model="logDrawerVisible" :title="`订单流转日志 - ${activeOrderId}`" size="36%">
      <el-timeline v-if="logs.length > 0">
        <el-timeline-item
          v-for="item in logs"
          :key="item.logId || `${item.operateTime}-${item.operatorId}`"
          :timestamp="item.operateTime"
        >
          <div class="log-title">{{ item.fromStatusText }} -> {{ item.toStatusText }}</div>
          <div class="log-meta">操作人：{{ item.operatorId }}</div>
          <div class="log-meta" v-if="item.remark">备注：{{ item.remark }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流转日志" />
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { ORDER_DETAIL_STATUS_MAP, ORDER_STATUS_MAP, ORDER_STATUS_OPTIONS } from "../../constants/order";

const activeTab = ref("orders");
const orderLoading = ref(false);
const orders = ref([]);
const orderTotal = ref(0);
const customerLoading = ref(false);
const customers = ref([]);
const customerTotal = ref(0);
const contractLoading = ref(false);
const contracts = ref([]);
const contractTotal = ref(0);
const logs = ref([]);
const logDrawerVisible = ref(false);
const activeOrderId = ref("");
const orderSubmitLoading = ref(false);
const customerSubmitLoading = ref(false);
const contractSubmitLoading = ref(false);

const orderQuery = reactive({ orderId: "", customerName: "", orderStatus: undefined, pageNo: 1, pageSize: 10 });
const customerQuery = reactive({ customerName: "", pageNo: 1, pageSize: 10 });
const contractQuery = reactive({ contractId: "", customerId: "", pageNo: 1, pageSize: 10 });

const createDefaultDetail = () => ({ productModel: "", airPermeability: 0, lengthReq: 0, widthReq: 0, craftReq: "" });
const orderForm = reactive({
  orderId: "",
  contractId: "",
  customerId: "",
  expectedDate: "",
  totalAmount: 0,
  orderStatus: 1,
  deliveryAddress: "",
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

const orderStatusText = (status) => ORDER_STATUS_MAP[status] || "未知";
const detailStatusText = (status) => ORDER_DETAIL_STATUS_MAP[status] || "未知";

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) return { records: payload.records, total: Number(payload.total || 0) };
  if (Array.isArray(payload)) return { records: payload, total: payload.length };
  return { records: [], total: 0 };
};

const normalizeRows = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.records && Array.isArray(payload.records)) return payload.records;
  if (payload?.list && Array.isArray(payload.list)) return payload.list;
  return [];
};

const generateOrderId = () => {
  const now = new Date();
  const pad = (n) => `${n}`.padStart(2, "0");
  return `ORD${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
};

const generateDetailId = (orderId, index) => {
  const suffix = `${index + 1}`.padStart(3, "0");
  return `${orderId.replace("ORD", "DET")}${suffix}`;
};

const fetchOrders = async () => {
  orderLoading.value = true;
  try {
    const res = await request.get("/api/orders/full", {
      params: {
        pageNo: orderQuery.pageNo,
        pageSize: orderQuery.pageSize,
        orderId: orderQuery.orderId || undefined,
        customerName: orderQuery.customerName || undefined,
        orderStatus: orderQuery.orderStatus
      }
    });
    const page = parsePage(res?.data ?? res);
    orders.value = page.records;
    orderTotal.value = page.total;
  } catch (error) {
    if (error?.response?.status === 404) {
      const [mastersRes, customersRes] = await Promise.all([
        request.get("/api/order-masters", {
          params: {
            pageNo: orderQuery.pageNo,
            pageSize: orderQuery.pageSize
          }
        }),
        request.get("/api/customers", {
          params: {
            pageNo: 1,
            pageSize: 1000,
            customerName: orderQuery.customerName || undefined
          }
        })
      ]);
      const masterPage = parsePage(mastersRes?.data ?? mastersRes);
      const customerRows = normalizeRows(customersRes?.data ?? customersRes);
      const customerMap = new Map(customerRows.map((item) => [item.customerId, item.customerName]));
      const filtered = masterPage.records
        .filter((item) => !orderQuery.orderId || String(item.orderId || "").includes(orderQuery.orderId))
        .filter((item) => orderQuery.orderStatus === undefined || item.orderStatus === orderQuery.orderStatus)
        .filter((item) => !orderQuery.customerName || String(customerMap.get(item.customerId) || "").includes(orderQuery.customerName))
        .map((item) => ({
          ...item,
          customerName: customerMap.get(item.customerId) || item.customerId,
          signDate: item.createdAt?.slice?.(0, 10) || "",
          deliveryAddress: item.remark || "",
          details: []
        }));
      orders.value = filtered;
      orderTotal.value = masterPage.total;
      ElMessage.warning("后端未提供 /api/orders/full，当前已降级展示基础订单数据");
      return;
    }
    orders.value = [];
    orderTotal.value = 0;
    ElMessage.error(error?.response?.data?.message || "订单数据加载失败");
  } finally {
    orderLoading.value = false;
  }
};

const fetchCustomers = async () => {
  customerLoading.value = true;
  try {
    const res = await request.get("/api/customers", {
      params: {
        pageNo: customerQuery.pageNo,
        pageSize: customerQuery.pageSize,
        customerName: customerQuery.customerName || undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    customers.value = page.records;
    customerTotal.value = page.total;
  } catch (error) {
    customers.value = [];
    customerTotal.value = 0;
    ElMessage.error(error?.response?.data?.message || "客户数据加载失败");
  } finally {
    customerLoading.value = false;
  }
};

const fetchContracts = async () => {
  contractLoading.value = true;
  try {
    const res = await request.get("/api/contracts", {
      params: {
        pageNo: contractQuery.pageNo,
        pageSize: contractQuery.pageSize,
        contractId: contractQuery.contractId || undefined,
        customerId: contractQuery.customerId || undefined
      }
    });
    const page = parsePage(res?.data ?? res);
    contracts.value = page.records;
    contractTotal.value = page.total;
  } catch (error) {
    contracts.value = [];
    contractTotal.value = 0;
    ElMessage.error(error?.response?.data?.message || "合同数据加载失败");
  } finally {
    contractLoading.value = false;
  }
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

const resetOrderQuery = () => {
  orderQuery.orderId = "";
  orderQuery.customerName = "";
  orderQuery.orderStatus = undefined;
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

const openLogDrawer = async (row) => {
  activeOrderId.value = row.orderId;
  logDrawerVisible.value = true;
  logs.value = [];
  try {
    const res = await request.get("/api/order-flow-logs", { params: { orderId: row.orderId } });
    const payload = res?.data ?? res;
    const list = Array.isArray(payload) ? payload : payload?.records || payload?.list || [];
    logs.value = list.map((item) => ({
      ...item,
      fromStatusText: orderStatusText(item.fromStatus),
      toStatusText: orderStatusText(item.toStatus)
    }));
  } catch (error) {
    if (error?.response?.status === 404) {
      ElMessage.warning("后端暂未提供流转日志接口 /api/order-flow-logs");
      return;
    }
    ElMessage.error(error?.response?.data?.message || "加载流转日志失败");
  }
};

const resetOrderForm = () => {
  orderForm.orderId = "";
  orderForm.contractId = "";
  orderForm.customerId = "";
  orderForm.expectedDate = "";
  orderForm.totalAmount = 0;
  orderForm.orderStatus = 1;
  orderForm.deliveryAddress = "";
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

const handleContractChange = (contractId) => {
  const selected = contracts.value.find((item) => item.contractId === contractId);
  if (!selected) return;
  orderForm.customerId = selected.customerId || "";
  if (!orderForm.deliveryAddress && selected.deliveryAddress) {
    orderForm.deliveryAddress = selected.deliveryAddress;
  }
};

const addDetailRow = () => orderForm.details.push(createDefaultDetail());
const removeDetailRow = (index) => {
  if (orderForm.details.length <= 1) return;
  orderForm.details.splice(index, 1);
};

const submitOrder = async () => {
  if (!orderForm.customerId) {
    ElMessage.warning("请选择合同号并自动带出客户ID");
    return;
  }
  if (!orderForm.contractId) {
    ElMessage.warning("请选择合同号");
    return;
  }
  if (!orderForm.details.some((item) => item.productModel)) {
    ElMessage.warning("至少填写一条有效明细（产品型号）");
    return;
  }
  if (orderForm.details.some((item) => item.productModel && (item.airPermeability === null || item.airPermeability === undefined))) {
    ElMessage.warning("已填写型号的明细必须填写透气量");
    return;
  }
  const payload = {
    orderId: orderForm.orderId || undefined,
    contractId: orderForm.contractId || undefined,
    customerId: orderForm.customerId,
    expectedDate: orderForm.expectedDate || undefined,
    totalAmount: orderForm.totalAmount,
    orderStatus: orderForm.orderStatus,
    deliveryAddress: orderForm.deliveryAddress || undefined,
    details: orderForm.details
      .filter((item) => item.productModel)
      .map((item) => ({
        productModel: item.productModel,
        airPermeability: item.airPermeability ?? 0,
        lengthReq: item.lengthReq ?? 0,
        widthReq: item.widthReq ?? 0,
        craftReq: item.craftReq || undefined
      }))
  };
  orderSubmitLoading.value = true;
  try {
    await request.post("/api/orders/full", payload);
    ElMessage.success("订单创建成功");
    resetOrderForm();
    orderQuery.pageNo = 1;
    fetchOrders();
  } catch (error) {
    if (error?.response?.status === 404) {
      const orderId = payload.orderId || generateOrderId();
      await request.post("/api/order-masters", {
        orderId,
        contractId: payload.contractId,
        customerId: payload.customerId,
        totalAmount: payload.totalAmount,
        expectedDate: payload.expectedDate,
        orderStatus: payload.orderStatus,
        remark: payload.deliveryAddress
      });
      await Promise.all(
        payload.details.map((item, index) =>
          request.post("/api/order-details", {
            detailId: generateDetailId(orderId, index),
            orderId,
            productModel: item.productModel,
            airPermeability: item.airPermeability ?? 0,
            lengthReq: item.lengthReq,
            widthReq: item.widthReq,
            craftReq: item.craftReq,
            detailStatus: 1
          })
        )
      );
      ElMessage.success("订单创建成功（基础接口降级）");
      resetOrderForm();
      orderQuery.pageNo = 1;
      fetchOrders();
      return;
    }
    ElMessage.error(error?.response?.data?.message || "订单创建失败");
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
    await request.post("/api/customers", {
      customerId: customerForm.customerId,
      customerName: customerForm.customerName,
      contactName: customerForm.contactName || undefined,
      contactPhone: customerForm.contactPhone || undefined,
      status: customerForm.status
    });
    ElMessage.success("客户创建成功");
    resetCustomerForm();
    customerQuery.pageNo = 1;
    fetchCustomers();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "客户创建失败");
  } finally {
    customerSubmitLoading.value = false;
  }
};

const submitContract = async () => {
  if (!contractForm.contractId || !contractForm.customerId || !contractForm.signDate) {
    ElMessage.warning("请填写合同号、客户ID、签订日期");
    return;
  }
  contractSubmitLoading.value = true;
  try {
    await request.post("/api/contracts", {
      contractId: contractForm.contractId,
      customerId: contractForm.customerId,
      contractAmount: contractForm.contractAmount,
      signDate: contractForm.signDate,
      deliveryAddress: contractForm.deliveryAddress || undefined,
      contractStatus: contractForm.contractStatus,
      remark: contractForm.remark || undefined
    });
    ElMessage.success("合同创建成功");
    resetContractForm();
    contractQuery.pageNo = 1;
    fetchContracts();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "合同创建失败");
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

.expand-wrap {
  padding: 8px 12px;
  background: #fafafa;
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

.log-title {
  font-weight: 600;
  color: #111827;
}

.log-meta {
  color: #6b7280;
  margin-top: 4px;
}
</style>
