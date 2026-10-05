package com.recruit.controller;

import com.recruit.entity.BrowseHistory;
import com.recruit.service.BrowseHistoryService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;

@RestController
@RequestMapping("/browse-history")
public class BrowseHistoryController extends BaseController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @GetMapping
    public Result<List<BrowseHistory>> getHistory(@RequestParam Long studentId) {
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        List<BrowseHistory> list = browseHistoryService.selectByStudentId(studentId);
        return Result.success(list);
    }

    @PostMapping
    public Result<String> addRecord(@RequestBody BrowseHistory record) {
        if (record.getStudentId() == null || record.getJobId() == null) {
            return Result.error("studentId 和 jobId 不能为空");
        }
        // 学生强制记录到自己名下
        if (Objects.equals(getCurrentRole(), 0)) {
            record.setStudentId(getCurrentUserId());
        }
        // 去重：同一学生同一岗位只保留最新一条
        BrowseHistory existing = browseHistoryService.lambdaQuery()
                .eq(BrowseHistory::getStudentId, record.getStudentId())
                .eq(BrowseHistory::getJobId, record.getJobId())
                .one();
        if (existing != null) {
            browseHistoryService.removeById(existing.getId());
        }
        record.setCreateTime(LocalDateTime.now());
        browseHistoryService.save(record);
        return Result.success("记录成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        BrowseHistory record = browseHistoryService.getById(id);
        if (record == null) {
            return Result.error(404, "记录不存在");
        }
        if (Objects.equals(getCurrentRole(), 0) && !Objects.equals(record.getStudentId(), getCurrentUserId())) {
            return Result.error(403, "无权删除他人记录");
        }
        browseHistoryService.removeById(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/clear")
    public Result<String> clearAll(@RequestParam Long studentId) {
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        browseHistoryService.lambdaUpdate()
                .eq(BrowseHistory::getStudentId, studentId)
                .remove();
        return Result.success("已清空");
    }

    @PostMapping("/batch-delete")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        if (Objects.equals(getCurrentRole(), 0)) {
            List<BrowseHistory> records = ids != null && !ids.isEmpty() ? browseHistoryService.listByIds(ids) : List.of();
            List<Long> ownIds = records.stream()
                    .filter(r -> Objects.equals(r.getStudentId(), getCurrentUserId()))
                    .map(BrowseHistory::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (ownIds.isEmpty()) {
                return Result.success("批量删除成功");
            }
            browseHistoryService.removeByIds(ownIds);
            return Result.success("批量删除成功");
        }
        browseHistoryService.removeByIds(ids);
        return Result.success("批量删除成功");
    }
}
