package com.yupi.springbootinit.service;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

/**
 * @author Administrator
 * @description AI服务
 */
public interface AIService {
    
    /**
     * 获取AI对代码的建议
     *
     * @param code 用户提交的代码
     * @param questionDescription 题目描述（可选）
     * @return AI的建议
     * @throws ApiException API异常
     * @throws NoApiKeyException API密钥缺失异常
     * @throws InputRequiredException 输入参数缺失异常
     */
    String getCodeSuggestion(String code, String questionDescription) throws ApiException, NoApiKeyException, InputRequiredException;

    /**
     * 获取AI对代码的建议（不包含题目信息）
     *
     * @param code 用户提交的代码
     * @return AI的建议
     * @throws ApiException API异常
     * @throws NoApiKeyException API密钥缺失异常
     * @throws InputRequiredException 输入参数缺失异常
     */
    default String getCodeSuggestion(String code) throws ApiException, NoApiKeyException, InputRequiredException {
        return getCodeSuggestion(code, null);
    }
    
    /**
     * 校验代码是否合法
     *
     * @param code 代码
     * @return 是否合法
     */
    boolean validCode(String code);
}

