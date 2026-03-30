<template>
  <view class="page">
    <view class="top-bar">
      <view>
        <view class="title">报工工作台</view>
        <view class="sub">当前用户：{{ userInfo.username || "未登录" }}</view>
      </view>
      <button size="mini" @click="logout">退出</button>
    </view>

    <view class="tabs">
      <view
        v-for="item in processOptions"
        :key="item.key"
        class="tab"
        :class="{ active: item.key === activeProcess }"
        @click="switchProcess(item.key)"
      >
        {{ item.label }}
      </view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: activeZone === 1 }" @click="switchZone(1)">生产区</view>
      <view class="tab" :class="{ active: activeZone === 2 }" @click="switchZone(2)">完工区</view>
    </view>

    <view class="actions">
      <button size="mini" @click="loadOrders">刷新</button>
    </view>

    <view v-if="loading" class="empty">加载中...</view>
    <view v-else-if="orders.length === 0" class="empty">暂无数据</view>

    <view v-else>
      <view v-for="item in orders" :key="getBatchNo(item)" class="card">
        <view class="row">
          <text class="label">批号</text>
          <text>{{ getBatchNo(item) }}</text>
        </view>
        <view class="row">
          <text class="label">状态</text>
          <text>{{ activeZone === 1 ? "待生产" : "已完工" }}</text>
        </view>
        <view class="row" v-if="item.operator_id">
          <text class="label">操作工号</text>
          <text>{{ item.operator_id }}</text>
        </view>
        <view class="ops">
          <button size="mini" @click="viewReport(item)">查看报工</button>
          <button v-if="activeZone === 1" size="mini" type="primary" @click="openReport(item)">提交报工</button>
        </view>
      </view>
    </view>

    <view v-if="reportVisible" class="mask" @click="closeReport">
      <view class="panel" @click.stop>
        <view class="panel-title">提交报工 - {{ currentProcess.label }}</view>
        <view class="panel-sub">批号：{{ currentBatchNo }}</view>

        <block v-if="activeProcess === 'weaving'">
          <input v-model="reportForm.machineId" class="input" placeholder="请输入机台号" />
          <input v-model="reportForm.operatorId" class="input" placeholder="请输入操作工号" />
          <input v-model="reportForm.materialBatchNo" class="input" placeholder="请输入原料批次号" />
          <input v-model="reportForm.tensionParams" class="input" placeholder="请输入张力参数" />
          <input v-model="reportForm.actualLength" class="input" placeholder="请输入实际产出米数" />
          <input v-model="reportForm.actualStartTime" class="input" placeholder="请输入开工时间(yyyy-MM-dd HH:mm:ss)" />
        </block>

        <block v-else-if="activeProcess === 'setting'">
          <input v-model="reportForm.operatorId" class="input" placeholder="请输入操作工号" />
          <input v-model="reportForm.actualTemp" class="input" placeholder="请输入实际温度" />
          <input v-model="reportForm.settingDuration" class="input" placeholder="请输入定型时长(分钟)" />
          <input v-model="reportForm.shrinkRate" class="input" placeholder="请输入收缩率" />
        </block>

        <block v-else-if="activeProcess === 'cutting'">
          <input v-model="reportForm.operatorId" class="input" placeholder="请输入操作工号" />
          <input v-model="reportForm.actualCutLen" class="input" placeholder="请输入实际裁切长度" />
          <input v-model="reportForm.actualCutWid" class="input" placeholder="请输入实际裁切宽度" />
          <input v-model="reportForm.wasteArea" class="input" placeholder="请输入损耗面积" />
        </block>

        <block v-else-if="activeProcess === 'splicing'">
          <input v-model="reportForm.operatorId" class="input" placeholder="请输入操作工号" />
          <input v-model="reportForm.spliceType" class="input" placeholder="请输入接头类型" />
          <input v-model="reportForm.jointStrength" class="input" placeholder="请输入接头强度" />
        </block>

        <block v-else>
          <input v-model="reportForm.operatorId" class="input" placeholder="请输入操作工号" />
          <input v-model="reportForm.finalLength" class="input" placeholder="请输入最终长度" />
          <input v-model="reportForm.finalWidth" class="input" placeholder="请输入最终宽度" />
          <input v-model="reportForm.meshDefectInfo" class="input" placeholder="请输入网病信息" />
          <input v-model="reportForm.qcTriggerFlag" class="input" placeholder="请输入质检标识(1/0)" />
        </block>

        <view class="ops">
          <button size="mini" @click="closeReport">取消</button>
          <button size="mini" type="primary" @click="submitReport" :disabled="submitting">
            {{ submitting ? "提交中..." : "确认提交" }}
          </button>
        </view>
      </view>
    </view>

    <view v-if="detailVisible" class="mask" @click="closeDetail">
      <view class="panel" @click.stop>
        <view class="panel-title">报工详情</view>
        <view class="panel-sub">批号：{{ detailBatchNo }}</view>
        <scroll-view scroll-y class="detail-body">
          <view v-for="row in detailRows" :key="row.label" class="row">
            <text class="label">{{ row.label }}</text>
            <text>{{ row.value }}</text>
          </view>
        </scroll-view>
        <view class="ops">
          <button size="mini" @click="closeDetail">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { clearAuth, request } from "../../utils/request";
import { getDefaultForm, getProcessOption, getReportGetUrl, getReportPostUrl, PROCESS_OPTIONS } from "../../utils/process";

const processOptions = PROCESS_OPTIONS;
const activeProcess = ref("weaving");
const activeZone = ref(1);
const loading = ref(false);
const submitting = ref(false);
const orders = ref([]);

const reportVisible = ref(false);
const detailVisible = ref(false);
const currentBatchNo = ref("");
const detailBatchNo = ref("");
const reportForm = reactive({});
const detailData = ref({});

const userInfo = reactive(uni.getStorageSync("userInfo") || {});
const currentProcess = computed(() => getProcessOption(activeProcess.value));

const detailSchema = {
  weaving: [
    ["weaving_batch_no", "织造批号"],
    ["machine_id", "机台号"],
    ["operator_id", "操作工号"],
    ["material_batch_no", "原料批次号"],
    ["tension_params", "张力参数"],
    ["actual_length", "实际产出米数"],
    ["actual_start_time", "开工时间"],
    ["actual_end_time", "完工时间"]
  ],
  setting: [
    ["setting_batch_no", "定型批号"],
    ["weaving_batch_no", "织造批号"],
    ["operator_id", "操作工号"],
    ["actual_temp", "实际温度"],
    ["setting_duration", "定型时长(分钟)"],
    ["shrink_rate", "收缩率"]
  ],
  cutting: [
    ["cut_batch_no", "裁网批号"],
    ["task_id", "裁网任务单号"],
    ["setting_batch_no", "定型批号"],
    ["detail_id", "明细订单号"],
    ["operator_id", "操作工号"],
    ["actual_cut_len", "实际裁切长度"],
    ["actual_cut_wid", "实际裁切宽度"],
    ["waste_area", "损耗面积"],
    ["cut_time", "报工时间"]
  ],
  splicing: [
    ["splice_batch_no", "插接批号"],
    ["cut_batch_no", "裁网批号"],
    ["operator_id", "操作工号"],
    ["splice_type", "接头类型"],
    ["joint_strength", "接头强度"]
  ],
  "sec-setting": [
    ["final_batch_no", "二次定型批号"],
    ["splice_batch_no", "插接批号"],
    ["operator_id", "操作工号"],
    ["final_length", "最终长度"],
    ["final_width", "最终宽度"],
    ["mesh_defect_info", "网病信息"],
    ["qc_trigger_flag", "质检触发标识"]
  ]
};

const detailRows = computed(() => {
  const schema = detailSchema[activeProcess.value] || [];
  const data = detailData.value || {};
  return schema.map(([key, label]) => ({
    label,
    value: data[key] ?? "-"
  }));
});

function resetForm() {
  const initial = getDefaultForm(activeProcess.value, userInfo.userId || "");
  Object.keys(reportForm).forEach((key) => delete reportForm[key]);
  Object.assign(reportForm, initial);
}

function getBatchNo(item) {
  return item[currentProcess.value.batchField] || "";
}

function switchProcess(key) {
  activeProcess.value = key;
  loadOrders();
}

function switchZone(zone) {
  activeZone.value = zone;
  loadOrders();
}

async function loadOrders() {
  loading.value = true;
  try {
    const data = await request({
      url: currentProcess.value.listUrl,
      method: "GET",
      data: {
        pageNo: 1,
        pageSize: 50,
        processStatus: activeZone.value
      }
    });
    orders.value = data.records || [];
  } catch (error) {
    orders.value = [];
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function openReport(item) {
  currentBatchNo.value = getBatchNo(item);
  resetForm();
  reportVisible.value = true;
}

function closeReport() {
  reportVisible.value = false;
}

function closeDetail() {
  detailVisible.value = false;
}

function normalizePayload() {
  const payload = { ...reportForm };
  const numKeys = ["actualLength", "actualTemp", "shrinkRate", "actualCutLen", "actualCutWid", "wasteArea", "finalLength", "finalWidth"];
  numKeys.forEach((key) => {
    if (payload[key] !== undefined && payload[key] !== "") payload[key] = Number(payload[key]);
  });
  if (payload.settingDuration !== undefined && payload.settingDuration !== "") payload.settingDuration = Number(payload.settingDuration);
  if (payload.qcTriggerFlag !== undefined && payload.qcTriggerFlag !== "") payload.qcTriggerFlag = Number(payload.qcTriggerFlag);
  return payload;
}

async function submitReport() {
  if (!currentBatchNo.value) return;
  submitting.value = true;
  try {
    await request({
      url: getReportPostUrl(activeProcess.value, currentBatchNo.value),
      method: "POST",
      data: normalizePayload()
    });
    uni.showToast({ title: "报工成功", icon: "success" });
    reportVisible.value = false;
    await loadOrders();
  } catch (error) {
    uni.showToast({ title: error.message || "报工失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
}

async function viewReport(item) {
  const batchNo = getBatchNo(item);
  if (!batchNo) return;
  detailBatchNo.value = batchNo;
  try {
    detailData.value = await request({ url: getReportGetUrl(activeProcess.value, batchNo), method: "GET" });
    detailVisible.value = true;
  } catch (error) {
    uni.showToast({ title: error.message || "获取详情失败", icon: "none" });
  }
}

async function logout() {
  try {
    await request({ url: "/api/auth/logout", method: "POST" });
  } catch (_) {
    // ignore
  }
  clearAuth();
  uni.reLaunch({ url: "/pages/login/login" });
}

onMounted(async () => {
  if (!uni.getStorageSync("token")) {
    uni.reLaunch({ url: "/pages/login/login" });
    return;
  }
  try {
    const me = await request({ url: "/api/auth/me", method: "GET" });
    Object.assign(userInfo, {
      userId: me.userId || userInfo.userId || "",
      username: me.username || userInfo.username || "",
      roleCode: me.roleCode || userInfo.roleCode || ""
    });
    uni.setStorageSync("userInfo", userInfo);
  } catch (_) {
    clearAuth();
    uni.reLaunch({ url: "/pages/login/login" });
    return;
  }
  await loadOrders();
});
</script>

<style scoped>
.page { padding: 14px; background: #f6f8fc; min-height: 100vh; }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.title { font-size: 20px; font-weight: 700; }
.sub { font-size: 12px; color: #6b7280; margin-top: 2px; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.tab { padding: 6px 10px; background: #fff; border-radius: 6px; border: 1px solid #dbe2ef; font-size: 13px; }
.tab.active { border-color: #2563eb; color: #2563eb; background: #eff6ff; }
.actions { margin-bottom: 8px; }
.card { background: #fff; border-radius: 8px; padding: 10px; margin-bottom: 8px; }
.row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; gap: 10px; }
.label { color: #6b7280; }
.ops { display: flex; gap: 8px; margin-top: 8px; }
.empty { text-align: center; color: #6b7280; margin-top: 40px; }
.mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.42); display: flex; align-items: flex-end; }
.panel { width: 100%; max-height: 88vh; background: #fff; border-radius: 14px 14px 0 0; padding: 12px; }
.panel-title { font-size: 16px; font-weight: 700; }
.panel-sub { font-size: 12px; color: #6b7280; margin: 6px 0 10px; }
.input { border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 10px; margin-bottom: 8px; font-size: 13px; }
.detail-body { max-height: 50vh; }
</style>