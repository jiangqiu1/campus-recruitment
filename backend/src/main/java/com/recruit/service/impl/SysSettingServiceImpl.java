package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.entity.SysSetting;
import com.recruit.mapper.SysSettingMapper;
import com.recruit.service.SysSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置服务实现
 */
@Service
public class SysSettingServiceImpl extends ServiceImpl<SysSettingMapper, SysSetting> implements SysSettingService {

    @Autowired
    private SysSettingMapper sysSettingMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> getSettingsByGroup(String groupKey) {
        List<SysSetting> list = sysSettingMapper.selectByGroup(groupKey);
        Map<String, Object> result = new HashMap<>();
        for (SysSetting s : list) {
            result.put(s.getSettingKey(), parseValue(s.getSettingValue()));
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Object>> getAllSettings() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        result.put("basic", getSettingsByGroup("basic"));
        result.put("security", getSettingsByGroup("security"));
        result.put("notification", getSettingsByGroup("notification"));
        return result;
    }

    @Override
    public void saveGroupSettings(String groupKey, Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = serializeValue(entry.getValue());

            SysSetting existing = sysSettingMapper.selectByGroupAndKey(groupKey, key);
            if (existing != null) {
                existing.setSettingValue(value);
                sysSettingMapper.updateById(existing);
            } else {
                SysSetting newSetting = new SysSetting();
                newSetting.setGroupKey(groupKey);
                newSetting.setSettingKey(key);
                newSetting.setSettingValue(value);
                sysSettingMapper.insert(newSetting);
            }
        }
    }

    /**
     * 将 JSON 字符串解析为合适的 Java 类型
     */
    private Object parseValue(String jsonValue) {
        if (jsonValue == null) return null;
        try {
            return objectMapper.readValue(jsonValue, Object.class);
        } catch (JsonProcessingException e) {
            // 不是合法 JSON，直接返回字符串
            return jsonValue;
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串
     */
    private String serializeValue(Object value) {
        // 如果是简单字符串且不包含特殊字符，加引号
        if (value instanceof String) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                return "\"" + value + "\"";
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
