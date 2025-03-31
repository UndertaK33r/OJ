<template>
  <div class="user-edit">
    <a-card class="edit-card" :bordered="false">
      <template #title>
        <a-space>
          <icon-edit />
          编辑个人资料
        </a-space>
      </template>

      <a-form :model="form" @submit="handleSubmit">
        <a-form-item
          field="userName"
          label="用户名"
          validate-trigger="blur"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input v-model="form.userName" placeholder="请输入用户名" />
        </a-form-item>

        <a-form-item field="userProfile" label="个人简介">
          <a-textarea
            v-model="form.userProfile"
            placeholder="请输入个人简介"
            :max-length="100"
            show-word-limit
          />
        </a-form-item>

        <a-form-item field="currentPassword" label="当前密码">
          <a-input-password
            v-model="form.currentPassword"
            placeholder="如需修改密码请输入当前密码"
          />
        </a-form-item>

        <a-form-item field="newPassword" label="新密码">
          <a-input-password
            v-model="form.newPassword"
            placeholder="请输入新密码"
          />
        </a-form-item>

        <a-form-item field="confirmPassword" label="确认新密码">
          <a-input-password
            v-model="form.confirmPassword"
            placeholder="请再次输入新密码"
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">
              保存修改
            </a-button>
            <a-button @click="handleCancel"> 取消 </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Message } from "@arco-design/web-vue";
import { IconEdit } from "@arco-design/web-vue/es/icon";
import { getLoginUser, updateUserInfo } from "@/api/user";

const router = useRouter();
const loading = ref(false);

const form = ref({
  userName: "",
  userProfile: "",
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const handleSubmit = async () => {
  try {
    if (
      form.value.newPassword &&
      form.value.newPassword !== form.value.confirmPassword
    ) {
      Message.error("两次输入的密码不一致");
      return;
    }

    loading.value = true;
    const updateData = {
      userName: form.value.userName,
      userProfile: form.value.userProfile,
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    };

    await updateUserInfo(updateData);
    Message.success("更新成功");
    router.push("/user/profile");
  } catch (error: any) {
    Message.error(error.message || "更新失败");
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  router.back();
};

onMounted(async () => {
  try {
    const userRes = await getLoginUser();
    if (userRes.data) {
      form.value.userName = userRes.data.userName || "";
      form.value.userProfile = userRes.data.userProfile || "";
    }
  } catch (error) {
    Message.error("获取用户信息失败");
  }
});
</script>

<style scoped>
.user-edit {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.edit-card {
  background: var(--color-bg-2);
  border-radius: 4px;
}
</style>
