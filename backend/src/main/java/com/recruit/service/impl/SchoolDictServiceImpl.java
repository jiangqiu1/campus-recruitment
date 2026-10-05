package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.SchoolDict;
import com.recruit.mapper.SchoolDictMapper;
import com.recruit.service.SchoolDictService;
import org.springframework.stereotype.Service;

/**
 * 学校字典 服务实现类
 */
@Service
public class SchoolDictServiceImpl extends ServiceImpl<SchoolDictMapper, SchoolDict> implements SchoolDictService {
}
