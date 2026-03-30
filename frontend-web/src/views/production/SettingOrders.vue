<template>
  <ProcessReportOrders
    title="定型订单"
    endpoint="/api/production/setting-orders"
    report-endpoint-prefix="setting-orders"
    batch-prop="setting_batch_no"
    batch-label="定型批次号"
    dialog-title="定型报工"
    :done-status="2"
    :columns="columns"
    :fields="fields"
  />
</template>

<script setup>
import ProcessReportOrders from "./ProcessReportOrders.vue";

const columns = [
  { prop: "setting_batch_no", label: "定型批次号", width: 170 },
  { prop: "weaving_batch_no", label: "织造批次号", width: 170 },
  { prop: "operator_id", label: "操作工号", width: 120 },
  { prop: "process_status", label: "状态", width: 100 },
  { prop: "completed_at", label: "完工时间", width: 170 }
];

const fields = [
  { key: "operatorId", label: "操作工号", type: "text", required: true, apiKey: "operator_id" },
  { key: "actualTemp", label: "实际温度", type: "number", required: true, precision: 2, apiKey: "actual_temp" },
  { key: "settingDuration", label: "定型时长(分钟)", type: "number", required: true, precision: 0, apiKey: "setting_duration" },
  { key: "shrinkRate", label: "收缩率", type: "number", required: true, precision: 2, apiKey: "shrink_rate" }
];
</script>
