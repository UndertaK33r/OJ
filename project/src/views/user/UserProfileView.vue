<template>
  <div class="user-profile">
    <a-card class="profile-card" :bordered="false">
      <template #title>
        <a-space>
          <icon-user />
          个人中心
        </a-space>
      </template>
      <a-descriptions :data="userInfo" layout="inline-vertical" bordered />
      <template #extra>
        <a-space>
          <a-button type="primary" @click="handleEdit">编辑资料</a-button>
          <a-button type="primary" status="success" @click="goToQuestionList">
            <template #icon>
              <icon-code />
            </template>
            返回做题
          </a-button>
        </a-space>
      </template>
    </a-card>

    <a-card class="history-card" :bordered="false" style="margin-top: 16px">
      <template #title>
        <a-space>
          <icon-history />
          做题历史
        </a-space>
      </template>
      <a-table
        :columns="columns"
        :data="historyData"
        :pagination="{ pageSize: 10 }"
      >
        <template #status="{ record }">
          <a-tag :color="getStatusColor(record.status, record.judgeInfo)">
            {{ getStatusText(record.status, record.judgeInfo) }}
          </a-tag>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Message } from "@arco-design/web-vue";
import { IconUser, IconHistory, IconCode } from "@arco-design/web-vue/es/icon";
import { getUserQuestionHistory } from "@/api/user";
import { getLoginUser } from "@/api/user";
import { QuestionControllerService } from "../../../generated";
import type { QuestionSubmitVO } from "../../../generated/models/QuestionSubmitVO";

const router = useRouter();

const userInfo = ref([
  {
    label: "用户名",
    value: "",
  },
  {
    label: "账号",
    value: "",
  },
  {
    label: "角色",
    value: "",
  },
  {
    label: "个人简介",
    value: "",
  },
  {
    label: "注册时间",
    value: "",
  },
  {
    label: "做题数量",
    value: "0",
  },
  {
    label: "通过率",
    value: "0%",
  },
]);

const columns = [
  {
    title: "题目",
    dataIndex: "questionName",
  },
  {
    title: "提交时间",
    dataIndex: "createTime",
  },
  {
    title: "编程语言",
    dataIndex: "language",
  },
  {
    title: "状态",
    dataIndex: "status",
    slotName: "status",
  },
];

const historyData = ref<QuestionSubmitVO[]>([]);

const handleEdit = () => {
  router.push("/user/edit");
};

const goToQuestionList = () => {
  router.push("/question");
};

const getStatusColor = (status: number, judgeInfo: any) => {
  // 如果状态不是2（成功），使用原来的颜色逻辑
  if (status !== 2) {
    switch (status) {
      case 3: // FAILED
        return "red";
      case 1: // RUNNING
        return "blue";
      case 0: // WAITING
        return "orange";
      default:
        return "gray";
    }
  }

  // 如果状态是2，根据judgeInfo判断
  if (judgeInfo?.message === "Accepted") {
    return "green";
  } else {
    return "red";
  }
};

const getStatusText = (status: number, judgeInfo: any) => {
  // 如果状态不是2（成功），使用原来的文本逻辑
  if (status !== 2) {
    switch (status) {
      case 3: // FAILED
        // 如果有judgeInfo，显示具体的错误信息
        if (judgeInfo?.message) {
          if (judgeInfo.message.startsWith("Compile Error:")) {
            return (
              "编译错误：" +
              judgeInfo.message.substring("Compile Error:".length).trim()
            );
          }
          return judgeInfo.message;
        }
        return "失败";
      case 1: // RUNNING
        return "运行中";
      case 0: // WAITING
        return "等待中";
      default:
        return "未知";
    }
  }

  // 如果状态是2，根据judgeInfo判断
  if (!judgeInfo) {
    return "未知";
  }

  switch (judgeInfo.message) {
    case "Accepted":
      return "通过";
    case "Wrong Answer":
      return "答案错误";
    case "Compile Error":
      return "编译错误";
    case "Memory Limit Exceeded":
      return "内存溢出";
    case "Time Limit Exceeded":
      return "超时";
    case "Runtime Error":
      return "运行错误";
    case "Output Limit Exceeded":
      return "输出溢出";
    case "Dangerous Operation":
      return "危险操作";
    case "System Error":
      return "系统错误";
    default:
      if (judgeInfo.message.startsWith("Compile Error:")) {
        return (
          "编译错误：" +
          judgeInfo.message.substring("Compile Error:".length).trim()
        );
      }
      return judgeInfo.message || "未知";
  }
};

onMounted(async () => {
  try {
    // 获取用户信息
    const userRes = await getLoginUser();
    const user = userRes.data;
    if (!user) {
      Message.error("获取用户信息失败");
      return;
    }
    userInfo.value[0].value = user.userName || "未设置";
    userInfo.value[1].value = user.userAccount || "";
    userInfo.value[2].value = user.userRole || "";
    userInfo.value[3].value = user.userProfile || "这个人很懒，什么都没写~";
    userInfo.value[4].value = user.createTime
      ? new Date(user.createTime).toLocaleDateString()
      : "未知";

    // 获取做题历史
    const historyRes = await getUserQuestionHistory();
    // 获取每个提交记录对应的题目信息
    const submissionList = historyRes.data || [];
    const questionPromises = submissionList.map(async (submission) => {
      try {
        if (!submission.questionId) {
          return {
            ...submission,
            questionName: "未知题目",
          };
        }
        const questionRes =
          await QuestionControllerService.getQuestionVoByIdUsingGet(
            submission.questionId
          );
        return {
          ...submission,
          questionName: questionRes.data?.title || "未知题目",
        };
      } catch (error) {
        console.error("获取题目信息失败", error);
        return {
          ...submission,
          questionName: "未知题目",
        };
      }
    });

    // 等待所有题目信息获取完成
    historyData.value = await Promise.all(questionPromises);

    // 计算做题数量和通过率
    const totalSubmissions = historyData.value.length;
    const acceptedSubmissions = historyData.value.filter(
      (item) => item.status === 2 && item.judgeInfo?.message === "Accepted"
    ).length;
    userInfo.value[5].value = totalSubmissions.toString();
    userInfo.value[6].value =
      totalSubmissions > 0
        ? ((acceptedSubmissions / totalSubmissions) * 100).toFixed(1) + "%"
        : "0%";
  } catch (error) {
    Message.error("获取用户信息失败");
  }
});
</script>

<style scoped>
.user-profile {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card,
.history-card {
  background: var(--color-bg-2);
  border-radius: 4px;
}
</style>
