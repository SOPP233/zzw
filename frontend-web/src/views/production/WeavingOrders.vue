<template>
  <ProcessOrders title="织造订单" endpoint="/api/production/weaving-orders" :columns="columns" />
</template>

<script setup>
import ProcessOrders from "./ProcessOrders.vue";

const columns = [
  { prop: "weaving_batch_no", label: "织造单号", width: 170 },
  { prop: "machine_id", label: "机台", width: 100 },
  { prop: "actual_length", label: "织造长度(m)", width: 120 },
  { prop: "actual_width", label: "织造宽度(m)", width: 120 },
  { prop: "process_status", label: "状态", width: 100 },
  { prop: "completed_at", label: "完工时间", width: 170 }
];
</script>
