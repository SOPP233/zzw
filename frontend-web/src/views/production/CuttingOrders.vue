<template>
  <ProcessReportOrders
    title="裁网订单"
    endpoint="/api/production/cutting-orders"
    report-endpoint-prefix="cutting-orders"
    batch-prop="cut_batch_no"
    batch-label="裁网批次号"
    dialog-title="裁网报工"
    :done-status="2"
    :columns="columns"
    :fields="fields"
  />
</template>

<script setup>
import ProcessReportOrders from "./ProcessReportOrders.vue";

const columns = [
  { prop: "cut_batch_no", label: "裁网批次号", width: 170 },
  { prop: "task_id", label: "裁网任务单号", width: 170 },
  { prop: "setting_batch_no", label: "定型批次号", width: 170 },
  { prop: "detail_id", label: "明细ID", width: 170 },
  { prop: "actual_cut_len", label: "裁切长度", width: 120 },
  { prop: "actual_cut_wid", label: "裁切宽度", width: 120 },
  { prop: "process_status", label: "状态", width: 100 }
];

const fields = [
  { key: "operatorId", label: "操作工号", type: "text", required: true, apiKey: "operator_id" },
  { key: "actualCutLen", label: "实际裁切长度", type: "number", required: true, precision: 2, apiKey: "actual_cut_len" },
  { key: "actualCutWid", label: "实际裁切宽度", type: "number", required: true, precision: 2, apiKey: "actual_cut_wid" },
  { key: "wasteArea", label: "损耗面积", type: "number", required: true, precision: 2, apiKey: "waste_area" }
];
</script>
