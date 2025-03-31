package com.yupi.springbootinit.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI代码分析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisRequest implements Serializable {

    /**
     * 用户提交的代码
     */
    private String code;

    /**
     * 题目ID
     */
    private Long questionId;

    private static final long serialVersionUID = 1L;
} 