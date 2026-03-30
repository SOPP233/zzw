<template>
  <el-card>
    <template #header>
      <span>系统用户管理</span>
    </template>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model.trim="query.username" placeholder="请输入用户名" clearable @keyup.enter="fetchUsers" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model.trim="query.realName" placeholder="请输入姓名" clearable @keyup.enter="fetchUsers" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleCode" clearable placeholder="全部角色">
          <el-option v-for="role in roleOptions" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchUsers">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="action-row">
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="userId" label="用户ID" min-width="150" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column prop="phone" label="电话" min-width="130" />
      <el-table-column prop="deptId" label="部门" min-width="120" />
      <el-table-column label="角色" min-width="220">
        <template #default="{ row }">
          <el-tag v-for="code in row.roleCodes || []" :key="code" size="small" class="mr8">{{ roleName(code) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="removeUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="fetchUsers"
        @size-change="onSizeChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="560px" destroy-on-close>
    <el-form :model="form" label-width="90px">
      <el-form-item label="用户名" required>
        <el-input v-model.trim="form.username" />
      </el-form-item>
      <el-form-item :label="isEdit ? '新密码' : '密码'" :required="!isEdit">
        <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空表示不修改密码' : '请输入密码'" />
      </el-form-item>
      <el-form-item label="姓名" required>
        <el-input v-model.trim="form.realName" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model.trim="form.phone" />
      </el-form-item>
      <el-form-item label="部门">
        <el-input v-model.trim="form.deptId" />
      </el-form-item>
      <el-form-item label="OpenID">
        <el-input v-model.trim="form.openId" />
      </el-form-item>
      <el-form-item label="UnionID">
        <el-input v-model.trim="form.unionId" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleCodes" multiple collapse-tags collapse-tags-tooltip style="width: 100%" placeholder="请选择角色">
          <el-option v-for="role in roleOptions" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const saving = ref(false);
const rows = ref([]);
const total = ref(0);
const roleOptions = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const editingUserId = ref("");

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  username: "",
  realName: "",
  status: null,
  roleCode: ""
});

const form = reactive({
  username: "",
  password: "",
  realName: "",
  phone: "",
  deptId: "",
  openId: "",
  unionId: "",
  status: 1,
  roleCodes: []
});

const parseData = (payload) => payload?.data ?? payload ?? {};

const parsePage = (payload) => {
  if (payload?.records && Array.isArray(payload.records)) {
    return { records: payload.records, total: Number(payload.total || 0) };
  }
  return { records: [], total: 0 };
};

const roleName = (code) => {
  const role = roleOptions.value.find((r) => r.roleCode === code);
  return role?.roleName || code;
};

const fetchRoleOptions = async () => {
  const res = await request.get("/api/system/roles");
  roleOptions.value = parseData(res);
};

const fetchUsers = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/system/users", {
      params: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        username: query.username || undefined,
        realName: query.realName || undefined,
        status: query.status ?? undefined,
        roleCode: query.roleCode || undefined
      }
    });
    const page = parsePage(parseData(res));
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载用户失败");
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.pageNo = 1;
  query.username = "";
  query.realName = "";
  query.status = null;
  query.roleCode = "";
  fetchUsers();
};

const onSizeChange = () => {
  query.pageNo = 1;
  fetchUsers();
};

const resetForm = () => {
  form.username = "";
  form.password = "";
  form.realName = "";
  form.phone = "";
  form.deptId = "";
  form.openId = "";
  form.unionId = "";
  form.status = 1;
  form.roleCodes = [];
};

const openCreate = () => {
  isEdit.value = false;
  editingUserId.value = "";
  resetForm();
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  editingUserId.value = row.userId;
  form.username = row.username || "";
  form.password = "";
  form.realName = row.realName || "";
  form.phone = row.phone || "";
  form.deptId = row.deptId || "";
  form.openId = row.openId || "";
  form.unionId = row.unionId || "";
  form.status = row.status ?? 1;
  form.roleCodes = [...(row.roleCodes || [])];
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.username || !form.realName) {
    ElMessage.warning("请填写用户名和姓名");
    return;
  }
  if (!isEdit.value && !form.password) {
    ElMessage.warning("请填写密码");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      username: form.username,
      password: form.password || undefined,
      realName: form.realName,
      phone: form.phone || undefined,
      deptId: form.deptId || undefined,
      openId: form.openId || undefined,
      unionId: form.unionId || undefined,
      status: form.status,
      roleCodes: form.roleCodes
    };
    if (isEdit.value) {
      await request.put(`/api/system/users/${editingUserId.value}`, payload);
    } else {
      await request.post("/api/system/users", payload);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchUsers();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "保存失败");
  } finally {
    saving.value = false;
  }
};

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username}？`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
    await request.delete(`/api/system/users/${row.userId}`);
    ElMessage.success("删除成功");
    fetchUsers();
  } catch (_) {
    // cancelled
  }
};

const bootstrap = async () => {
  await fetchRoleOptions();
  await fetchUsers();
};

bootstrap();
</script>

<style scoped>
.search-bar {
  margin-bottom: 12px;
}

.action-row {
  margin-bottom: 10px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.mr8 {
  margin-right: 6px;
  margin-bottom: 4px;
}
</style>
