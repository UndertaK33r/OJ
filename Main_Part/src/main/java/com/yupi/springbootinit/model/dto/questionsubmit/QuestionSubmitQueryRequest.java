package com.yupi.springbootinit.model.dto.questionsubmit;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 代码
     */
    private Integer status;


    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 用户id
     */
    private Long userId;



    private static final long serialVersionUID = 1L;
}