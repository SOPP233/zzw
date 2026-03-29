<template>
  <ProcessOrders title="裁网订单" endpoint="/api/production/cutting-orders" :columns="columns" />
</template>

<script setup>
import ProcessOrders from "./ProcessOrders.vue";

const columns = [
  { prop: "cut_batch_no", label: "裁网单号", width: 170 },
  { prop: "setting_batch_no", label: "定型单号", width: 170 },
  { prop: "detail_id", label: "明细ID", width: 170 },
  { prop: "actual_cut_len", label: "裁切长度", width: 120 },
  { prop: "actual_cut_wid", label: "裁切宽度", width: 120 },
  { prop: "process_status", label: "状态", width: 100 }
];
</script>
