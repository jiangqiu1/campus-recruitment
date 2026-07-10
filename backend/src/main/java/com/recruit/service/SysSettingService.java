package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.SysSetting;

import java.util.Map;

/**
 * 系统设置服务接口
 */
public interface SysSettingService extends IService<SysSetting> {

    /**
     * 获取指定分组的设置（以 Map 形式返回）
     */
    Map<String, Object> getSettingsByGroup(String groupKey);

    /**
     * 获取所有设置（按分组返回，用于前端展示）
     */
    Map<String, Map<String, Object>> getAllSettings();

    /**
     * 批量保存某个分组的设置
     */
    void saveGroupSettings(String groupKey, Map<String, Object> settings);
}
