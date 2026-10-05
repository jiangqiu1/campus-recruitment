package com.recruit.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.recruit.entity.SchoolDict;
import com.recruit.service.SchoolDictService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典控制器
 * 提供学校字典等基础数据，需登录访问
 */
@RestController
@RequestMapping("/dict")
public class DictController extends BaseController {

    @Autowired
    private SchoolDictService schoolDictService;

    /**
     * 学校字典列表（可按名称模糊搜索）
     */
    @GetMapping("/schools")
    public Result<List<SchoolDict>> getSchools(@RequestParam(required = false) String keyword) {
        QueryWrapper<SchoolDict> qw = new QueryWrapper<SchoolDict>()
                .eq("status", 1)
                .orderByAsc("id");
        // 注意：like 的第三参数会被立即求值，判空必须放在条件外
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like("name", keyword.trim());
        }
        return Result.success(schoolDictService.list(qw));
    }
}
