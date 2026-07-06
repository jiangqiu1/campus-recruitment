package com.recruit.enums;

/**
 * 用户角色枚举
 */
public enum UserRoleEnum {
    STUDENT(0, "学生"),
    TEACHER(1, "教师"),
    HR(2, "企业HR"),
    ADMIN(3, "管理员");

    private final int value;
    private final String label;

    UserRoleEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static UserRoleEnum fromValue(Integer value) {
        if (value == null) return null;
        for (UserRoleEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }

    public static boolean isTeacher(Integer value) {
        return TEACHER.value == (value != null ? value : -1);
    }

    public static boolean isHr(Integer value) {
        return HR.value == (value != null ? value : -1);
    }

    public static boolean isAdmin(Integer value) {
        return ADMIN.value == (value != null ? value : -1);
    }
}
