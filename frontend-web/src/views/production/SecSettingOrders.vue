<template>
  <ProcessOrders title="二次定型订单" endpoint="/api/production/sec-setting-orders" :columns="columns" />
</template>

<script setup>
import ProcessOrders from "./ProcessOrders.vue";

const columns = [
  { prop: "final_batch_no", label: "二定单号", width: 170 },
  { prop: "splice_batch_no", label: "插接单号", width: 170 },
  { prop: "final_length", label: "最终长度", width: 120 },
  { prop: "final_width", label: "最终宽度", width: 120 },
  { prop: "process_status", label: "状态", width: 100 },
  { prop: "completed_at", label: "完工时间", width: 170 }
];
</script>
