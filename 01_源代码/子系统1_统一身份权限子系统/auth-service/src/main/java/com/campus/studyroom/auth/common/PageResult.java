package com.campus.studyroom.auth.common;

import java.util.List;

/**
 * 分页返回结构，对应契约中的 total / pageNum / pageSize / list。
 *
 * @param <T> 列表元素类型
 * @author 成员1
 */
public class PageResult<T> {

    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
