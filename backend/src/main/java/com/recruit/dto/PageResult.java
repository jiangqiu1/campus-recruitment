package com.recruit.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回结构
 * 所有列表接口统一返回此格式，前端无需适配多种分页结构
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总页数 */
    private int pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setList(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPage((int) page.getCurrent());
        r.setSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        r.setPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return r;
    }
}
