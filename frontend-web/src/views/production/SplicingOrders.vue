<template>
  <ProcessReportOrders
    title="插接订单"
    endpoint="/api/production/splicing-orders"
    report-endpoint-prefix="splicing-orders"
    batch-prop="splice_batch_no"
    batch-label="插接批次号"
    dialog-title="插接报工"
    :done-status="2"
    :columns="columns"
    :fields="fields"
  />
</template>

<script setup>
import ProcessReportOrders from "./ProcessReportOrders.vue";

const columns = [
  { prop: "splice_batch_no", label: "插接批次号", width: 170 },
  { prop: "cut_batch_no", label: "裁网批次号", width: 170 },
  { prop: "operator_id", label: "操作工号", width: 120 },
  { prop: "splice_type", label: "插接类型", width: 130 },
  { prop: "process_status", label: "状态", width: 100 }
];

const fields = [
  { key: "operatorId", label: "操作工号", type: "text", required: true, apiKey: "operator_id" },
  { key: "spliceType", label: "插接类型", type: "text", required: true, apiKey: "splice_type" },
  { key: "jointStrength", label: "强度标识", type: "text", required: true, apiKey: "joint_strength" }
];
</script>
