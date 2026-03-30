<template>
  <ProcessReportOrders
    title="二次定型订单"
    endpoint="/api/production/sec-setting-orders"
    report-endpoint-prefix="sec-setting-orders"
    batch-prop="final_batch_no"
    batch-label="二次定型批次号"
    dialog-title="二次定型报工"
    :done-status="2"
    :columns="columns"
    :fields="fields"
  />
</template>

<script setup>
import ProcessReportOrders from "./ProcessReportOrders.vue";

const columns = [
  { prop: "final_batch_no", label: "二定批次号", width: 170 },
  { prop: "splice_batch_no", label: "插接批次号", width: 170 },
  { prop: "final_length", label: "最终长度", width: 120 },
  { prop: "final_width", label: "最终宽度", width: 120 },
  { prop: "process_status", label: "状态", width: 100 },
  { prop: "completed_at", label: "完工时间", width: 170 }
];

const fields = [
  { key: "operatorId", label: "操作工号", type: "text", required: true, apiKey: "operator_id" },
  { key: "finalLength", label: "最终长度", type: "number", required: true, precision: 2, apiKey: "final_length" },
  { key: "finalWidth", label: "最终宽度", type: "number", required: true, precision: 2, apiKey: "final_width" },
  { key: "meshDefectInfo", label: "网病参数", type: "text", required: false, apiKey: "mesh_defect_info" },
  {
    key: "qcTriggerFlag",
    label: "触发质检",
    type: "select",
    required: true,
    apiKey: "qc_trigger_flag",
    options: [
      { label: "是", value: 1 },
      { label: "否", value: 0 }
    ]
  }
];
</script>
