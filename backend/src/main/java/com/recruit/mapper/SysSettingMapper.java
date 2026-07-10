package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.SysSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统设置 Mapper 接口
 */
@Mapper
public interface SysSettingMapper extends BaseMapper<SysSetting> {

    /**
     * 根据分组查询所有设置
     */
    @Select("SELECT * FROM sys_settings WHERE group_key = #{groupKey} ORDER BY id")
    List<SysSetting> selectByGroup(@Param("groupKey") String groupKey);

    /**
     * 查询某个具体配置
     */
    @Select("SELECT * FROM sys_settings WHERE group_key = #{groupKey} AND setting_key = #{settingKey}")
    SysSetting selectByGroupAndKey(@Param("groupKey") String groupKey, @Param("settingKey") String settingKey);
}
