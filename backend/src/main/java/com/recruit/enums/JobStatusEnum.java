package com.recruit.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 岗位状态枚举
 */
public enum JobStatusEnum {
    DRAFT(0, "草稿"),
    ACTIVE(1, "招聘中"),
    CLOSED(2, "已关闭"),
    PAUSED(3, "已暂停");

    private final int value;
    private final String label;

    private static final Map<Integer, JobStatusEnum> MAP = new HashMap<>();
    static {
        for (JobStatusEnum e : values()) MAP.put(e.value, e);
    }

    JobStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static JobStatusEnum fromValue(Integer value) {
        return value == null ? null : MAP.get(value);
    }
}
