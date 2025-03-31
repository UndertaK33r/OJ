<template>
  <div id="viewQuestionView">
    <a-row :gutter="[24, 24]">
      <a-col :md="12" :xs="24">
        <a-tabs v-model:activeKey="activeTab" class="custom-tabs">
          <a-tab-pane key="question" title="题目">
            <a-card v-if="question" class="question-card" :bordered="false">
              <div class="question-header">
                <h2 class="question-title">{{ question.title }}</h2>
                <a-space wrap class="question-tags">
                  <a-tag
                    v-for="(tag, index) of question.tags"
                    :key="index"
                    color="arcoblue"
                    >{{ tag }}
                  </a-tag>
                </a-space>
              </div>
              <a-divider />
              <a-descriptions
                title="判题条件"
                :column="{ xs: 1, md: 2, lg: 3 }"
                class="judge-conditions"
                bordered
              >
                <a-descriptions-item label="时间限制">
                  <a-space>
                    <icon-timer />
                    {{ question.judgeConfig.timeLimit ?? 0 }} ms
                  </a-space>
                </a-descriptions-item>
                <a-descriptions-item label="内存限制">
                  <a-space>
                    <icon-computer />
                    {{ question.judgeConfig.memoryLimit ?? 0 }} MB
                  </a-space>
                </a-descriptions-item>
                <a-descriptions-item label="堆栈限制">
                  <a-space>
                    <icon-storage />
                    {{ question.judgeConfig.stackLimit ?? 0 }} MB
                  </a-space>
                </a-descriptions-item>
              </a-descriptions>
              <div class="question-content">
                <MdViewer :value="question.content || ''" />
              </div>
            </a-card>
          </a-tab-pane>
          <a-tab-pane key="comment" title="AI分析">
            <a-card :bordered="false" class="comment-card">
              <template #title>
                <a-space>
                  <icon-robot />
                  AI代码分析
                </a-space>
              </template>
              <a-spin :loading="aiLoading">
                <div v-if="aiAnalysis" class="ai-analysis">
                  <MdViewer :value="aiAnalysis" />
                </div>
                <a-empty v-else description="暂无AI分析结果">
                  <template #image>
                    <icon-robot
                      :style="{
                        fontSize: '48px',
                        color: '#C2C7D4',
                      }"
                    />
                  </template>
                </a-empty>
              </a-spin>
              <template #extra>
                <a-button
                  type="primary"
                  @click="getAiAnalysis"
                  :loading="aiLoading"
                >
                  获取AI分析
                </a-button>
              </template>
            </a-card>
          </a-tab-pane>
          <a-tab-pane key="status" title="状态">
            <a-card :bordered="false" class="status-card">
              <a-table
                :columns="submitColumns"
                :data="submitList"
                :pagination="{
                  showTotal: true,
                  pageSize: submitSearchParams.pageSize,
                  current: submitSearchParams.current,
                  total: submitTotal,
                  showJumper: true,
                  size: 'medium',
                }"
                @page-change="onSubmitPageChange"
                :bordered="false"
                :stripe="true"
                class="custom-table"
              >
                <template #status="{ record }">
                  <a-tag
                    :color="getStatusColor(record.status, record.judgeInfo)"
                    class="status-tag"
                  >
                    <a-space>
                      <icon-check
                        v-if="record.judgeInfo?.message === 'Accepted'"
                      />
                      <icon-close v-else-if="record.judgeInfo?.message" />
                      <icon-sync v-else class="spinning" />
                      {{ getStatusText(record.status, record.judgeInfo) }}
                    </a-space>
                  </a-tag>
                </template>
                <template #judgeInfo="{ record }">
                  <p v-if="record.judgeInfo" class="judge-info">
                    {{ record.judgeInfo.message }}
                  </p>
                </template>
                <template #createTime="{ record }">
                  <span class="time-text">
                    {{
                      moment(record.createTime).format("YYYY-MM-DD HH:mm:ss")
                    }}
                  </span>
                </template>
              </a-table>
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </a-col>
      <a-col :md="12" :xs="24">
        <a-card :bordered="false" class="code-editor-card">
          <a-form :model="form" layout="inline" class="language-form">
            <a-form-item
              field="language"
              label="编程语言"
              class="language-select"
            >
              <a-select
                v-model="form.language"
                :style="{ width: '320px' }"
                placeholder="选择编程语言"
                size="large"
              >
                <a-option>java</a-option>
                <a-option>cpp</a-option>
                <a-option>go</a-option>
              </a-select>
            </a-form-item>
          </a-form>
          <div class="editor-container">
            <CodeEditor
              :value="form.code as string"
              :language="form.language"
              :handle-change="changeCode"
            />
          </div>
          <div class="submit-container">
            <a-button
              type="primary"
              size="large"
              @click="doSubmit"
              class="submit-button"
            >
              <template #icon>
                <icon-send />
              </template>
              提交代码
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import {
  onMounted,
  ref,
  withDefaults,
  defineProps,
  onBeforeUnmount,
} from "vue";
import {
  Question,
  QuestionControllerService,
  QuestionSubmitAddRequest,
  QuestionVO,
  QuestionSubmitVO,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import CodeEditor from "@/components/CodeEditor.vue";
import MdViewer from "@/components/MdViewer.vue";
import moment from "moment";
import {
  IconTimer,
  IconComputer,
  IconStorage,
  IconCheck,
  IconClose,
  IconSync,
  IconSend,
  IconRobot,
} from "@arco-design/web-vue/es/icon";

interface Props {
  id: string;
}

const props = withDefaults(defineProps<Props>(), {
  id: () => "",
});

const question = ref<QuestionVO>();

// 提交记录相关
const submitList = ref<QuestionSubmitVO[]>([]);
const submitTotal = ref(0);
const submitSearchParams = ref({
  questionId: undefined as number | undefined,
  pageSize: 10,
  current: 1,
});

const submitColumns = [
  {
    title: "提交状态",
    slotName: "status",
  },
  {
    title: "判题信息",
    slotName: "judgeInfo",
  },
  {
    title: "编程语言",
    dataIndex: "language",
  },
  {
    title: "提交时间",
    slotName: "createTime",
  },
];

const loadData = async () => {
  const res = await QuestionControllerService.getQuestionVoByIdUsingGet(
    props.id as any
  );
  if (res.code === 0) {
    question.value = res.data;
    // 加载提交记录
    await loadSubmitData();
  } else {
    message.error("加载失败，" + res.message);
  }
};

const form = ref<QuestionSubmitAddRequest>({
  language: "java",
  code: "",
});

// 轮询定时器
const pollingTimer = ref<number>();

// 当前激活的标签页
const activeTab = ref("question");

// 开始轮询判题状态
const startPolling = () => {
  // 如果已经有定时器在运行，先清除它
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
  }

  // 每秒查询一次判题状态
  pollingTimer.value = setInterval(async () => {
    await loadSubmitData();

    // 如果所有提交都已经有了最终结果，停止轮询
    const allFinished = submitList.value.every(
      (submit) =>
        submit.judgeInfo && (submit.judgeInfo.message || submit.judgeInfo.error)
    );
    if (allFinished) {
      clearInterval(pollingTimer.value);
      pollingTimer.value = undefined;
    }
  }, 1000);
};

// 停止轮询
const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
    pollingTimer.value = undefined;
  }
};

/**
 * 提交代码
 */
const doSubmit = async () => {
  if (!question.value?.id) {
    return;
  }

  const res = await QuestionControllerService.doQuestionSubmitUsingPost({
    ...form.value,
    questionId: question.value.id,
  });
  if (res.code === 0) {
    message.success("提交成功");
    // 重新加载提交记录
    await loadSubmitData();
    // 开始轮询判题状态
    startPolling();
    // 切换到状态标签页
    activeTab.value = "status";
  } else {
    message.error("提交失败," + res.message);
  }
};

/**
 * 页面加载时，请求数据
 */
onMounted(() => {
  loadData();
});

const changeCode = (value: string) => {
  form.value.code = value;
};

// 加载提交记录
const loadSubmitData = async () => {
  if (!question.value?.id) {
    return;
  }
  submitSearchParams.value.questionId = question.value.id;
  const res = await QuestionControllerService.listQuestionSubmitByPageUsingPost(
    submitSearchParams.value
  );
  if (res.code === 0) {
    submitList.value = res.data.records;
    submitTotal.value = res.data.total;
    // 打印出第一条记录的判题信息
    if (submitList.value.length > 0) {
      console.log("判题信息:", submitList.value[0].judgeInfo);
    }
  } else {
    message.error("加载失败，" + res.message);
  }
};

// 监听页面变化
const onSubmitPageChange = (page: number) => {
  submitSearchParams.value.current = page;
  loadSubmitData();
};

// 获取提交状态的颜色
const getStatusColor = (status: number, judgeInfo?: any) => {
  if (!judgeInfo) {
    return "orange"; // 判题中
  }

  if (judgeInfo.message === "Accepted") {
    return "green"; // 判题通过
  } else if (judgeInfo.message) {
    return "red"; // 判题失败，有具体错误信息
  } else if (judgeInfo.error) {
    return "red"; // 编译错误等
  }

  return "orange"; // 其他情况继续判题中
};

// 获取提交状态的文本
const getStatusText = (status: number, judgeInfo?: any) => {
  if (!judgeInfo) {
    return "判题中";
  }

  if (judgeInfo.message === "Accepted") {
    return "判题通过";
  } else if (judgeInfo.message === "Wrong Answer") {
    return "代码错误";
  } else if (judgeInfo.message) {
    return judgeInfo.message; // 显示具体的错误信息
  } else if (judgeInfo.error) {
    return judgeInfo.error; // 显示编译错误等信息
  }

  return "判题中";
};

// AI分析相关
const aiAnalysis = ref<string>();
const aiLoading = ref(false);

// 获取AI分析
const getAiAnalysis = async () => {
  if (!form.value.code || !question.value?.id) {
    message.warning("请先输入代码");
    return;
  }
  aiLoading.value = true;
  try {
    const res = await QuestionControllerService.getAiCodeAnalysisUsingPost({
      code: form.value.code,
      questionId: question.value.id,
    });
    if (res.code === 0) {
      aiAnalysis.value = res.data;
      // 获取成功后自动切换到AI分析标签页
      activeTab.value = "comment";
    } else {
      message.error("获取AI分析失败," + res.message);
    }
  } catch (error) {
    console.error("AI分析错误:", error);
    message.error("获取AI分析失败," + error);
  } finally {
    aiLoading.value = false;
  }
};

// 组件卸载前清除定时器
onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style scoped>
#viewQuestionView {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.custom-tabs {
  background: var(--color-bg-2);
  border-radius: 8px;
  padding: 16px;
}

.question-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.question-header {
  margin-bottom: 24px;
}

.question-title {
  font-size: 24px;
  margin-bottom: 16px;
  color: var(--color-text-1);
}

.question-tags {
  margin-top: 8px;
}

.judge-conditions {
  margin: 24px 0;
  background: var(--color-fill-2);
  border-radius: 4px;
  padding: 16px;
}

.question-content {
  margin-top: 24px;
}

.status-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.custom-table {
  margin-top: 16px;
}

.status-tag {
  padding: 6px 12px;
  border-radius: 4px;
  font-weight: 500;
}

.judge-info {
  margin: 0;
  color: var(--color-text-2);
}

.time-text {
  color: var(--color-text-3);
  font-size: 13px;
}

.code-editor-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.language-form {
  margin-bottom: 16px;
}

.language-select {
  width: 100%;
}

.editor-container {
  flex: 1;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.submit-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.submit-button {
  min-width: 200px;
  height: 44px;
  font-size: 16px;
  transition: all 0.3s;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

:deep(.arco-space-horizontal .arco-space-item) {
  margin-bottom: 0 !important;
}

.comment-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.ai-analysis {
  padding: 16px;
  background: var(--color-fill-2);
  border-radius: 4px;
  margin-top: 16px;
}
</style>
