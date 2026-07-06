package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.BrowseHistory;
import com.recruit.mapper.BrowseHistoryMapper;
import com.recruit.service.BrowseHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements BrowseHistoryService {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Override
    public List<BrowseHistory> selectByStudentId(Long studentId) {
        return browseHistoryMapper.selectByStudentId(studentId);
    }
}
