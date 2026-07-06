package com.recruit.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 投递状态枚举
 */
public enum DeliveryStatusEnum {
    PENDING(0, "待查看", "#F59E0B"),
    VIEWED(1, "已查看", "#165DFF"),
    INTERVIEW(2, "面试中", "#165DFF"),
    ACCEPTED(3, "已录用", "#8B5CF6"),
    REJECTED(4, "不合适", "#EF4444");

    private final int value;
    private final String label;
    private final String color;

    private static final Map<Integer, DeliveryStatusEnum> MAP = new HashMap<>();
    static {
        for (DeliveryStatusEnum e : values()) MAP.put(e.value, e);
    }

    DeliveryStatusEnum(int value, String label, String color) {
        this.value = value;
        this.label = label;
        this.color = color;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }
    public String getColor() { return color; }

    public static DeliveryStatusEnum fromValue(Integer value) {
        return value == null ? null : MAP.get(value);
    }
}
