package com.recruit.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 在 Controller 方法上标注后，AOP 自动记录操作日志到 operation_log 表
 *
 * 使用示例：
 * {@code @LogOperation("审核通过岗位变更申请")}
 * {@code @PutMapping("/{id}/approve")}
 * public Result<String> approve(@PathVariable Long id, HttpServletRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作描述
     * 例如："审核通过岗位变更申请"、"关闭岗位"
     */
    String value();

    /**
     * 操作类型
     * 默认 OPERATION，可用于后续按类型筛选
     */
    String type() default "OPERATION";

    /**
     * 是否记录方法参数中的业务 ID
     * 默认 true，会自动提取第一个 @PathVariable 或名为 id/xxxId 的参数值
     */
    boolean recordTargetId() default true;
}
