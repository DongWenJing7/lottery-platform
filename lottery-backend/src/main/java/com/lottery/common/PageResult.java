package com.lottery.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        return r;
    }
}
