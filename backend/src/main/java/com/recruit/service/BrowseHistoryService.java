package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.BrowseHistory;

import java.util.List;

public interface BrowseHistoryService extends IService<BrowseHistory> {
    List<BrowseHistory> selectByStudentId(Long studentId);
}
