package com.recruit.controller;

import com.recruit.entity.BrowseHistory;
import com.recruit.service.BrowseHistoryService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/browse-history")
public class BrowseHistoryController extends BaseController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @GetMapping
    public Result<List<BrowseHistory>> getHistory(@RequestParam Long studentId) {
        List<BrowseHistory> list = browseHistoryService.selectByStudentId(studentId);
        return Result.success(list);
    }

    @PostMapping
    public Result<String> addRecord(@RequestBody BrowseHistory record) {
        if (record.getStudentId() == null || record.getJobId() == null) {
            return Result.error("studentId 和 jobId 不能为空");
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
        browseHistoryService.removeById(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/clear")
    public Result<String> clearAll(@RequestParam Long studentId) {
        browseHistoryService.lambdaUpdate()
                .eq(BrowseHistory::getStudentId, studentId)
                .remove();
        return Result.success("已清空");
    }

    @PostMapping("/batch-delete")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        browseHistoryService.removeByIds(ids);
        return Result.success("批量删除成功");
    }
}
