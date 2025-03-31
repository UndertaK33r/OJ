package com.example.sandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {


    /**
     * 执行信息
     */
    private String message;
    /**
     * 消耗内存KB
     */
    private Long memory;
    /**
     * 消耗时间
     */
    private Long time;
}
