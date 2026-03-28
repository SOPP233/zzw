<template>
  <div class="page">
    <el-result
      icon="warning"
      title="无访问权限"
      sub-title="当前角色没有此功能的访问权限，请联系管理员或切换角色。"
    />
  </div>
</template>

<style scoped>
.page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 88px);
}
</style>

