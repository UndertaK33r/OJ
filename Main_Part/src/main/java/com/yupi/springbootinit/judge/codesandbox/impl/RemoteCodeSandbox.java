package com.yupi.springbootinit.judge.codesandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.StringUtils;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.judge.codesandbox.CodeSandbox;
import com.yupi.springbootinit.judge.codesandbox.model.ExecuteCodeRequest;
import com.yupi.springbootinit.judge.codesandbox.model.ExecuteCodeResponse;
import com.yupi.springbootinit.judge.codesandbox.model.JudgeInfo;
import com.yupi.springbootinit.model.enums.JudgeInfoMessageEnum;
import com.yupi.springbootinit.model.enums.QuestionSubmitStatusEnum;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 远程代码沙箱（实际调用接口的沙箱）
 */
public class RemoteCodeSandbox implements CodeSandbox {

    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "this_is_a_secret";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        
        // 1. 基本参数校验
        if (executeCodeRequest == null || StringUtils.isBlank(executeCodeRequest.getCode())) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "请求参数错误");
        }
        
        // 2. 检查文件类型（可以根据实际需求扩展支持的语言）
        String language = executeCodeRequest.getLanguage();
        if (!isValidLanguage(language)) {
            return getErrorResponse(JudgeInfoMessageEnum.COMPILE_ERROR, "不支持的文件类型");
        }
        
        // 3. 调用远程沙箱
        String url = "http://localhost:8088/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr;
        try {
            responseStr = HttpUtil.createPost(url)
                    .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                    .body(json)
                    .execute()
                    .body();
        } catch (Exception e) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "远程沙箱调用失败: " + e.getMessage());
        }
        
        if (StringUtils.isBlank(responseStr)) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "远程沙箱响应为空");
        }
        
        // 4. 解析响应结果
        ExecuteCodeResponse executeCodeResponse;
        try {
            executeCodeResponse = JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
        } catch (Exception e) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "响应结果解析失败");
        }
        
        // 5. 完善判题信息
        if (executeCodeResponse.getJudgeInfo() == null) {
            // 根据输出和错误信息判断具体的错误类型
            String errorMessage = executeCodeResponse.getErrorMessage();
            JudgeInfoMessageEnum judgeInfoMessage = JudgeInfoMessageEnum.COMPILE_ERROR;
            
            if (errorMessage != null) {
                if (errorMessage.contains("compilation failed") || errorMessage.contains("syntax error")) {
                    judgeInfoMessage = JudgeInfoMessageEnum.COMPILE_ERROR;
                } else if (errorMessage.contains("runtime error")) {
                    judgeInfoMessage = JudgeInfoMessageEnum.RUNTIME_ERROR;
                } else if (errorMessage.contains("time limit")) {
                    judgeInfoMessage = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
                } else if (errorMessage.contains("memory limit")) {
                    judgeInfoMessage = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
                }
            }
            
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage(judgeInfoMessage.getValue());
            judgeInfo.setMemory(0L);
            judgeInfo.setTime(0L);
            executeCodeResponse.setJudgeInfo(judgeInfo);
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        }
        
        return executeCodeResponse;
    }
    
    private boolean isValidLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return false;
        }
        // 这里可以根据实际支持的语言类型进行扩展
        return language.equalsIgnoreCase("java") 
            || language.equalsIgnoreCase("python")
            || language.equalsIgnoreCase("cpp")
            || language.equalsIgnoreCase("c");
    }
    
    private ExecuteCodeResponse getErrorResponse(JudgeInfoMessageEnum messageEnum, String errorMessage) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(messageEnum.getValue());
        judgeInfo.setMemory(0L);
        judgeInfo.setTime(0L);
        response.setJudgeInfo(judgeInfo);
        response.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        response.setErrorMessage(errorMessage);
        return response;
    }
}
