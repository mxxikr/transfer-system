package com.transfer.system.policy;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PagingPolicy {
    private final int defaultPage;
    private final int defaultSize;
    private final int maxSize;
    private final String transactionSortField;

    public PagingPolicy(
            @Value("${paging.default.page}") int defaultPage,
            @Value("${paging.default.size}") int defaultSize,
            @Value("${paging.max.size}") int maxSize,
            @Value("${paging.transaction.sort.field}") String transactionSortField) {
        this.defaultPage = defaultPage;
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
        this.transactionSortField = transactionSortField;
    }

    /**
     * 페이지 크기 유효성 검사 및 정책 적용
     */
    public int getValidatedSize(int requestedSize) {
        if (requestedSize <= 0) {
            return defaultSize;
        }
        return Math.min(requestedSize, maxSize);
    }

    /**
     * 페이지 번호 유효성 검사
     */
    public int getValidatedPage(Integer requestedPage) {
        return (requestedPage != null && requestedPage >= 0) ? requestedPage : defaultPage;
    }
}