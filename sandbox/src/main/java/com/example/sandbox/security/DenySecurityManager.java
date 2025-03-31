package com.example.sandbox.security;

import java.security.Permission;

/**
 * 禁用安全管理器
 */
public class DenySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("禁止访问");
    }
}
