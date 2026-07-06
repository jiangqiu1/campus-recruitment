package com.recruit.aspect;

import com.recruit.annotation.LogOperation;
import com.recruit.entity.OperationLog;
import com.recruit.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 标注了 @LogOperation 的 Controller 方法执行后，自动记录操作日志
 */
@Slf4j
@Aspect
@Component
public class LogOperationAspect {

    @Autowired(required = false)
    private OperationLogService operationLogService;

    @Around("@annotation(com.recruit.annotation.LogOperation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation logOp = method.getAnnotation(LogOperation.class);

        // 执行原方法
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            // 异常也记录日志（失败场景）
            saveLog(logOp, joinPoint, false, e.getMessage());
            throw e;
        }

        long cost = System.currentTimeMillis() - start;
        // 记录成功日志
        saveLog(logOp, joinPoint, true, null);
        log.debug("操作日志 [{}] 完成，耗时 {}ms", logOp.value(), cost);

        return result;
    }

    private void saveLog(LogOperation logOp, ProceedingJoinPoint joinPoint, boolean success, String errorMsg) {
        if (operationLogService == null) return;

        try {
            // 获取当前请求（用于取 IP）
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs != null ? attrs.getRequest() : null;

            // 解析业务 ID
            String targetId = resolveTargetId(joinPoint, logOp);

            // 获取当前用户 ID
            Long userId = null;
            if (request != null) {
                Object uid = request.getAttribute("userId");
                if (uid != null) {
                    userId = uid instanceof Long ? (Long) uid : Long.valueOf(uid.toString());
                }
                // IP 地址
                String ip = request.getRemoteAddr();
                if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
                    ip = "127.0.0.1";
                }

                OperationLog logEntry = new OperationLog();
                logEntry.setUserId(userId);
                logEntry.setOperationType(logOp.type() + ":" + logOp.value());
                logEntry.setTargetId(targetId);
                logEntry.setIpAddress(ip);
                logEntry.setCreateTime(LocalDateTime.now());

                operationLogService.save(logEntry);
            }
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    /**
     * 从方法参数中自动提取业务 ID
     * 优先提取名为 id / xxxId 的 @PathVariable 参数
     */
    private String resolveTargetId(ProceedingJoinPoint joinPoint, LogOperation logOp) {
        if (!logOp.recordTargetId()) return null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        if (paramNames == null || paramValues == null) return null;

        for (int i = 0; i < paramNames.length; i++) {
            if (paramValues[i] == null) continue;
            String name = paramNames[i].toLowerCase();
            // 匹配 id / xxxId 模式的参数
            if ("id".equals(name) || name.endsWith("id")) {
                return paramValues[i].toString();
            }
        }
        return null;
    }
}
