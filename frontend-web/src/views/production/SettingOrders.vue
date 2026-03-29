<template>
  <ProcessOrders title="定型订单" endpoint="/api/production/setting-orders" :columns="columns" />
</template>

<script setup>
import ProcessOrders from "./ProcessOrders.vue";

const columns = [
  { prop: "setting_batch_no", label: "定型单号", width: 170 },
  { prop: "weaving_batch_no", label: "织造单号", width: 170 },
  { prop: "operator_id", label: "操作工", width: 120 },
  { prop: "process_status", label: "状态", width: 100 },
  { prop: "completed_at", label: "完工时间", width: 170 }
];
</script>
