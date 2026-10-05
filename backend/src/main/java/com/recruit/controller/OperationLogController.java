package com.recruit.controller;

import com.recruit.entity.OperationLog;
import com.recruit.service.OperationLogService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 * 只有管理员可以访问
 */
@RestController
@RequestMapping("/operation-logs")
public class OperationLogController extends BaseController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有操作日志
     *
     * @return 操作日志列表
     */
    @GetMapping
    public Result<List<OperationLog>> getAllLogs() {
        requireAdmin();
        List<OperationLog> logs = operationLogService.list();
        return Result.success(logs);
    }

    /**
     * 根据ID获取操作日志
     *
     * @param id 日志ID
     * @return 操作日志实体
     */
    @GetMapping("/{id}")
    public Result<OperationLog> getLogById(@PathVariable Long id) {
        requireAdmin();
        OperationLog log = operationLogService.getById(id);
        if (log == null) {
            return Result.error(404, "操作日志不存在");
        }
        return Result.success(log);
    }

    /**
     * 根据用户ID查询操作日志
     *
     * @param userId 用户ID
     * @return 操作日志列表
     */
    @GetMapping("/by-user/{userId}")
    public Result<List<OperationLog>> getLogsByUserId(@PathVariable Long userId) {
        requireAdmin();
        List<OperationLog> logs = operationLogService.selectByUserId(userId);
        return Result.success(logs);
    }

    /**
     * 根据操作类型查询操作日志
     *
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    @GetMapping("/by-operation-type/{operationType}")
    public Result<List<OperationLog>> getLogsByOperationType(@PathVariable String operationType) {
        requireAdmin();
        List<OperationLog> logs = operationLogService.selectByOperationType(operationType);
        return Result.success(logs);
    }

    /**
     * 根据用户ID和操作类型查询操作日志
     *
     * @param userId 用户ID
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    @GetMapping("/by-user-and-operation-type")
    public Result<List<OperationLog>> getLogsByUserIdAndOperationType(
            @RequestParam Long userId,
            @RequestParam String operationType) {
        List<OperationLog> logs = operationLogService.selectByUserIdAndOperationType(userId, operationType);
        return Result.success(logs);
    }

    /**
     * 根据时间范围查询操作日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    @GetMapping("/by-time-range")
    public Result<List<OperationLog>> getLogsByTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        List<OperationLog> logs = operationLogService.selectByTimeRange(startTime, endTime);
        return Result.success(logs);
    }

    /**
     * 记录操作日志（内部调用，通常由AOP自动记录）
     *
     * @param params 包含userId、operationType、targetId、ipAddress的参数
     * @return 记录结果
     */
    @PostMapping
    public Result<String> logOperation(@RequestBody Map<String, Object> params) {
        requireAdmin();
        Long userId = Long.valueOf(params.get("userId").toString());
        String operationType = params.get("operationType").toString();
        String targetId = params.get("targetId").toString();
        String ipAddress = params.get("ipAddress").toString();

        boolean success = operationLogService.logOperation(userId, operationType, targetId, ipAddress);
        if (!success) {
            return Result.error("记录操作日志失败");
        }

        return Result.success("操作日志记录成功");
    }

    /**
     * 删除操作日志（软删除）
     *
     * @param id 日志ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteLog(@PathVariable Long id) {
        requireAdmin();
        boolean success = operationLogService.removeById(id);
        if (!success) {
            return Result.error("删除失败");
        }

        return Result.success("操作日志删除成功");
    }

    /**
     * 清理指定时间之前的日志（用于日志归档）
     *
     * @param beforeTime 截止时间
     * @return 删除的行数
     */
    @DeleteMapping("/cleanup")
    public Result<Map<String, Integer>> cleanupLogs(@RequestParam LocalDateTime beforeTime) {
        requireAdmin();
        int count = operationLogService.cleanupLogsBeforeTime(beforeTime);

        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("deletedCount", count);

        return Result.success("日志清理完成", result);
    }

    /**
     * 统计用户操作次数（用于数据分析）
     *
     * @param userId 用户ID
     * @return 操作次数统计（按操作类型分组）
     */
    @GetMapping("/statistics/count-by-user/{userId}")
    public Result<Map<String, Integer>> countByUserIdAndGroupByOperationType(@PathVariable Long userId) {
        requireAdmin();
        Map<String, Integer> statistics = operationLogService.countByUserIdAndGroupByOperationType(userId);
        return Result.success(statistics);
    }

    /**
     * 获取最近的操作日志（用于数据大屏）
     *
     * @param limit 数量限制
     * @return 最近的操作日志列表
     */
    @GetMapping("/recent")
    public Result<List<OperationLog>> getRecentLogs(@RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin();
        List<OperationLog> logs = operationLogService.lambdaQuery()
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT " + limit)
                .list();

        return Result.success(logs);
    }
}
