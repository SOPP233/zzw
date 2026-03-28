<template>
  <div class="page">
    <el-row :gutter="12" class="mb-12">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-title">平均订单交付周期(天)</div>
          <div class="kpi-value">{{ overview.avgDeliveryCycleDays }}</div>
          <div class="kpi-sub">订单录入至成品入库</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-title">本周良品率</div>
          <div class="kpi-value">{{ overview.currentWeekYieldRate }}%</div>
          <div class="kpi-sub">裁网+插接工序综合</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-title">在制批次数</div>
          <div class="kpi-value">{{ overview.inProgressBatchCount }}</div>
          <div class="kpi-sub">状态=加工中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-title">逾期订单数</div>
          <div class="kpi-value warn">{{ overview.overdueOrderCount }}</div>
          <div class="kpi-sub">预期交付已超期</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mb-12">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>平均订单交付周期趋势</template>
          <el-table :data="deliveryCycleSeries" size="small" border height="280">
            <el-table-column prop="period" label="周期" min-width="110" />
            <el-table-column prop="avgDays" label="平均交付周期(天)" min-width="130" />
            <el-table-column label="趋势">
              <template #default="{ row }">
                <el-progress :percentage="row.progressPct" :show-text="false" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>车间周度良品率对比</template>
          <el-table :data="yieldRateSeries" size="small" border height="280">
            <el-table-column prop="week" label="周次" min-width="90" />
            <el-table-column prop="workshop" label="车间" min-width="100" />
            <el-table-column prop="yieldRate" label="良品率(%)" min-width="120" />
            <el-table-column label="达标">
              <template #default="{ row }">
                <el-tag :type="row.yieldRate >= 95 ? 'success' : 'warning'" size="small">
                  {{ row.yieldRate >= 95 ? "达标" : "预警" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>执行层任务预警清单</template>
      <el-table :data="alerts" border>
        <el-table-column prop="alertType" label="预警类型" min-width="150" />
        <el-table-column prop="targetCode" label="对象编号" min-width="170" />
        <el-table-column prop="message" label="预警内容" min-width="280" />
        <el-table-column prop="eventTime" label="发生时间" min-width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const overview = ref({
  avgDeliveryCycleDays: "-",
  currentWeekYieldRate: "-",
  inProgressBatchCount: "-",
  overdueOrderCount: "-"
});

const deliveryCycleSeries = ref([]);
const yieldRateSeries = ref([]);
const alerts = ref([]);

const calcProgress = (value, maxValue) => {
  if (!maxValue || maxValue <= 0) return 0;
  return Math.round((value / maxValue) * 100);
};

const loadOverview = async () => {
  try {
    // API 骨架：统计概览指标
    const res = await request.get("/api/stats/overview");
    const data = res?.data ?? res;
    overview.value = {
      avgDeliveryCycleDays: data.avgDeliveryCycleDays ?? "-",
      currentWeekYieldRate: data.currentWeekYieldRate ?? "-",
      inProgressBatchCount: data.inProgressBatchCount ?? "-",
      overdueOrderCount: data.overdueOrderCount ?? "-"
    };
  } catch (error) {
    if (error?.response?.status === 404) {
      overview.value = {
        avgDeliveryCycleDays: 16.8,
        currentWeekYieldRate: 96.2,
        inProgressBatchCount: 14,
        overdueOrderCount: 3
      };
      return;
    }
    ElMessage.error("加载统计概览失败");
  }
};

const loadDeliveryCycleSeries = async () => {
  try {
    // API 骨架：交付周期趋势（按周/月）
    const res = await request.get("/api/stats/delivery-cycle-trend");
    const rows = (res?.data ?? res) || [];
    const max = Math.max(...rows.map((i) => i.avgDays), 1);
    deliveryCycleSeries.value = rows.map((item) => ({
      ...item,
      progressPct: calcProgress(item.avgDays, max)
    }));
  } catch (error) {
    if (error?.response?.status === 404) {
      const mock = [
        { period: "2026-W08", avgDays: 20.2 },
        { period: "2026-W09", avgDays: 18.4 },
        { period: "2026-W10", avgDays: 17.1 },
        { period: "2026-W11", avgDays: 16.8 }
      ];
      const max = Math.max(...mock.map((i) => i.avgDays), 1);
      deliveryCycleSeries.value = mock.map((item) => ({ ...item, progressPct: calcProgress(item.avgDays, max) }));
      return;
    }
    ElMessage.error("加载交付周期趋势失败");
  }
};

const loadYieldRateSeries = async () => {
  try {
    // API 骨架：周度良品率对比
    const res = await request.get("/api/stats/weekly-yield-rate");
    yieldRateSeries.value = (res?.data ?? res) || [];
  } catch (error) {
    if (error?.response?.status === 404) {
      yieldRateSeries.value = [
        { week: "2026-W10", workshop: "一车间", yieldRate: 95.6 },
        { week: "2026-W10", workshop: "二车间", yieldRate: 96.9 },
        { week: "2026-W11", workshop: "一车间", yieldRate: 94.8 },
        { week: "2026-W11", workshop: "二车间", yieldRate: 97.2 }
      ];
      return;
    }
    ElMessage.error("加载周度良品率失败");
  }
};

const loadAlerts = async () => {
  try {
    // API 骨架：执行层任务预警
    const res = await request.get("/api/stats/alerts");
    alerts.value = (res?.data ?? res) || [];
  } catch (error) {
    if (error?.response?.status === 404) {
      alerts.value = [
        {
          alertType: "交付风险",
          targetCode: "ORD20260328018",
          message: "订单预计逾期 2 天，请协调加班班次",
          eventTime: "2026-03-28 10:20:11"
        },
        {
          alertType: "质量预警",
          targetCode: "BAT260328012",
          message: "插接工序当周良品率低于 95%",
          eventTime: "2026-03-28 09:42:07"
        }
      ];
      return;
    }
    ElMessage.error("加载预警清单失败");
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadDeliveryCycleSeries(), loadYieldRateSeries(), loadAlerts()]);
});
</script>

<style scoped>
.page {
  padding: 4px;
}

.mb-12 {
  margin-bottom: 12px;
}

.kpi-title {
  color: #6b7280;
  font-size: 13px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.kpi-sub {
  margin-top: 8px;
  color: #9ca3af;
  font-size: 12px;
}

.warn {
  color: #dc2626;
}
</style>

