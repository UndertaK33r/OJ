<template>
  <a-row id="globalHeader" align="center" :wrap="false">
    <a-col flex="auto">
      <a-menu
        mode="horizontal"
        :selectedKeys="selectedKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item
          key="0"
          :style="{ padding: 0, marginRight: '38px' }"
          disabled
        >
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" />
            <div class="title">汪code</div>
          </div>
        </a-menu-item>
        <a-menu-item v-for="item in visibleRoutes" :key="item.path"
          >{{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="120px">
      <a-dropdown trigger="hover">
        <div class="user-dropdown">
          <a-avatar :size="32" style="margin-right: 8px">
            <IconUser />
          </a-avatar>
          <span>{{ store.state.user?.loginUser?.userName ?? "未登录" }}</span>
        </div>
        <template #content>
          <a-doption @click="goToUserProfile">
            <template #icon>
              <icon-user />
            </template>
            <template #default>个人中心</template>
          </a-doption>
          <a-doption @click="handleLogout">
            <template #icon>
              <icon-export />
            </template>
            <template #default>退出登录</template>
          </a-doption>
        </template>
      </a-dropdown>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { routes } from "../router/routes";
import { useRoute, useRouter } from "vue-router";
import { computed, ref } from "vue";
import { useStore } from "vuex";
import checkAccess from "@/access/checkAccess";
import ACCESS_ENUM from "@/access/accessEnum";
import { IconUser, IconExport } from "@arco-design/web-vue/es/icon";
import { Message } from "@arco-design/web-vue";

const router = useRouter();
const store = useStore();
const loginUser = store.state.user.loginUser;

//默认主页
const selectedKeys = ref(["/"]);

//展示在菜单的路由数组
const visibleRoutes = computed(() => {
  return routes.filter((item, index) => {
    if (item.meta?.hideInMenu) {
      return false;
    }
    //根据权限过滤菜单
    if (
      !checkAccess(store.state.user.loginUser, item?.meta?.access as string)
    ) {
      return false;
    }
    return true;
  });
});

//路由跳转后，更新选中的菜单项
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path];
});

setTimeout(() => {
  store.dispatch("user/getLoginUser", {
    userName: "administrator",
    userRole: ACCESS_ENUM.ADMIN,
  });
}, 3000);

const doMenuClick = (key: string) => {
  router.push({ path: key });
};

// 跳转到个人中心
const goToUserProfile = () => {
  router.push("/user/profile");
};

// 处理退出登录
const handleLogout = async () => {
  try {
    await store.dispatch("user/userLogout");
    Message.success("退出登录成功");
    router.push("/user/login");
  } catch (error) {
    Message.error("退出登录失败");
  }
};
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title-bar {
  color: #444;
  margin-left: 16px;
}

.logo {
  height: 48px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 4px;
  border-radius: 4px;
}

.user-dropdown:hover {
  background: rgba(var(--primary-6), 0.1);
}
</style>
