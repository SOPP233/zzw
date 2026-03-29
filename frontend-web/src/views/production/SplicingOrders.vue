<template>
  <ProcessOrders title="插接订单" endpoint="/api/production/splicing-orders" :columns="columns" />
</template>

<script setup>
import ProcessOrders from "./ProcessOrders.vue";

const columns = [
  { prop: "splice_batch_no", label: "插接单号", width: 170 },
  { prop: "cut_batch_no", label: "裁网单号", width: 170 },
  { prop: "operator_id", label: "操作工", width: 120 },
  { prop: "splice_type", label: "插接工艺", width: 130 },
  { prop: "process_status", label: "状态", width: 100 }
];
</script>
