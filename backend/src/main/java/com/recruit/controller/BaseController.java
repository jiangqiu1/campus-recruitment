package com.recruit.controller;

import com.recruit.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 控制器基类 — 提供当前登录用户的便捷访问
 * JwtInterceptor 已在请求属性中注入 userId / role / username
 */
public class BaseController {

    /**
     * 获取当前请求
     */
    protected HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(401, "无法获取请求上下文");
        }
        return attrs.getRequest();
    }

    /**
     * 获取当前登录用户ID（从JWT解析，由JwtInterceptor注入）
     */
    protected Long getCurrentUserId() {
        Object val = getRequest().getAttribute("userId");
        if (val == null) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return val instanceof Long ? (Long) val : Long.valueOf(val.toString());
    }

    /**
     * 获取当前登录用户角色
     * 0=学生, 1=教师, 2=HR, 3=管理员
     */
    protected Integer getCurrentRole() {
        Object val = getRequest().getAttribute("role");
        if (val == null) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return val instanceof Integer ? (Integer) val : Integer.valueOf(val.toString());
    }

    /**
     * 校验当前用户是否为教师角色，不是则抛出403异常
     */
    protected void requireTeacher() {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 1)) {
            throw new BusinessException(403, "仅教师可执行此操作");
        }
    }

    /**
     * 校验当前用户是否为HR角色，不是则抛出403异常
     */
    protected void requireHr() {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 2)) {
            throw new BusinessException(403, "仅HR可执行此操作");
        }
    }

    /**
     * 校验当前用户是否为管理员角色
     */
    protected void requireAdmin() {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 3)) {
            throw new BusinessException(403, "仅管理员可执行此操作");
        }
    }
}
