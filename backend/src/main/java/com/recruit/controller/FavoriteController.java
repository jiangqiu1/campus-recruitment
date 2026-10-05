package com.recruit.controller;

import com.recruit.entity.Favorite;
import com.recruit.mapper.FavoriteMapper;
import com.recruit.service.FavoriteService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * 收藏控制器 - 学生端
 */
@RestController
@RequestMapping("/favorites")
public class FavoriteController extends BaseController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteMapper favoriteMapper;

    /**
     * 获取学生收藏的岗位列表
     */
    @GetMapping
    public Result<List<Favorite>> getFavorites(@RequestParam(required = false) Long studentId) {
        if (studentId == null) {
            return Result.success(List.of());
        }
        List<Favorite> list = favoriteService.lambdaQuery()
                .eq(Favorite::getStudentId, studentId)
                .orderByDesc(Favorite::getCreateTime)
                .list();
        return Result.success(list);
    }

    /**
     * 添加收藏
     */
    @PostMapping
    public Result<String> addFavorite(@RequestBody Map<String, Object> params) {
        Long studentId = params.get("studentId") != null ? Long.valueOf(params.get("studentId").toString()) : null;
        Long jobId = params.get("jobId") != null ? Long.valueOf(params.get("jobId").toString()) : null;
        if (studentId == null || jobId == null) {
            return Result.error("studentId和jobId不能为空");
        }
        // 学生强制收藏到自己名下
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        // 优先恢复逻辑删除的记录（绕过 MyBatis-Plus 的 deleted=0 过滤）
        int restored = favoriteMapper.restoreFavorite(studentId, jobId);
        if (restored > 0) {
            return Result.success("收藏成功");
        }
        // 检查是否已活跃收藏
        var exists = favoriteService.lambdaQuery()
                .eq(Favorite::getStudentId, studentId)
                .eq(Favorite::getJobId, jobId)
                .count();
        if (exists > 0) {
            return Result.success("已收藏");
        }
        Favorite fav = new Favorite();
        fav.setStudentId(studentId);
        fav.setJobId(jobId);
        favoriteService.save(fav);
        return Result.success("收藏成功");
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{jobId}")
    public Result<String> removeFavorite(@PathVariable Long jobId,
                                          @RequestParam(required = false) Long studentId) {
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        if (studentId == null) {
            return Result.error("studentId不能为空");
        }
        favoriteService.lambdaUpdate()
                .eq(Favorite::getStudentId, studentId)
                .eq(Favorite::getJobId, jobId)
                .remove();
        return Result.success("已取消收藏");
    }
}
