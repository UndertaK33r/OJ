package com.yupi.springbootinit.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.yupi.springbootinit.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * @author Administrator
 * @description AI服务实现类
 */
@Service
@Slf4j
public class AIServiceImpl implements AIService {

    /**
     * AI API密钥
     */
    @Value("${ai.key}")
    private String apiKey;

    /**
     * AI模型名称
     */
    private static final String MODEL_NAME = "qwen-plus";

    /**
     * 系统提示语（无题目信息）
     */
    private static final String SYSTEM_PROMPT = "你是一个新手编程学习者的编程老师。请对用户提交的代码进行分析，给出简单改进建议。" +
            "注意：不要直接提供完整的代码解决方案，而是提供指导性的建议，帮助用户理解问题并自主改进代码，如果代码可以正常运行，不用给出建议给出一些编程相关小知识。" +
            "先进行代码分析，然后改进建议，然后小知识"+
            "建议应该包括：1. 代码中存在的问题 2. 可以改进的地方 3. 编程最佳实践的建议";
    
    @Override
    public String getCodeSuggestion(String code, String questionDescription) throws ApiException, NoApiKeyException, InputRequiredException {
        if (!validCode(code)) {
            throw new InputRequiredException("代码格式不合法");
        }
        try {
            Generation gen = new Generation();
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(SYSTEM_PROMPT)
                    .build();
            
            String userPrompt;
            if (StringUtils.isBlank(questionDescription)) {
                userPrompt = String.format("请分析这段代码并给出建议：\n```\n%s\n```", code);
            } else {
                userPrompt = String.format("题目描述：%s\n\n用户提交的代码：\n```\n%s\n```\n\n请根据题目描述分析这段代码并给出建议。",
                        questionDescription, code);
            }
            
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userPrompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(MODEL_NAME)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
                    
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("调用AI服务异常", e);
            throw e;
        }
    }

    @Override
    public boolean validCode(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        return true;
    }
} 