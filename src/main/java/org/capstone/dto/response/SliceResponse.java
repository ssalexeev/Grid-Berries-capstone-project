package org.capstone.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponse<T> {
    private final List<T> content;
    private final int currentPage;
    private final int pageSize;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final boolean isFirst;
    private final boolean isLast;

    public SliceResponse(Slice<T> slice) {
        this.content = slice.getContent();
        this.currentPage = slice.getNumber();
        this.pageSize = slice.getSize();
        this.hasNext = slice.hasNext();
        this.hasPrevious = slice.hasPrevious();
        this.isFirst = slice.isFirst();
        this.isLast = slice.isLast();
    }
}
