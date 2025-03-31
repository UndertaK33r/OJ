package com.example.sandbox.controller;
import com.example.sandbox.CodeSandbox;
import com.example.sandbox.JavaDockerCodeSandboxImpl;
import com.example.sandbox.JavaNativeCodeSandbox;
import com.example.sandbox.model.ExecuteCodeRequest;
import com.example.sandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {
    // 鉴权
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "this_is_a_secret";

    @Resource
    private JavaNativeCodeSandbox JavaNativeCodeSandbox;

//    @GetMapping("/health")
//    public String health() {
//        return "OK";
//    }

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request
    , HttpServletResponse response) {
        //基本认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空 ");
        }
        //原生沙箱
        ExecuteCodeResponse executeCodeResponse = JavaNativeCodeSandbox.executeCode(executeCodeRequest);
        //docker沙箱

        return executeCodeResponse;

    }
}
