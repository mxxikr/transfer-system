package com.transfer.system.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PagingPolicyTest {

    private PagingPolicy pagingPolicy;

    private final int TEST_DEFAULT_PAGE = 0;
    private final int TEST_DEFAULT_SIZE = 20;
    private final int TEST_MAX_SIZE = 100;
    private final String TEST_SORT_FIELD = "createdTimeStamp";

    @BeforeEach
    void setUp() {
        pagingPolicy = new PagingPolicy(
            TEST_DEFAULT_PAGE,
            TEST_DEFAULT_SIZE,
            TEST_MAX_SIZE,
            TEST_SORT_FIELD
        );
    }

    // ========================= 페이지 번호 검증 =========================
    @Nested
    class GetValidatedPageTest {

        /**
         * 페이지 검증 - null이면 기본 값 반환
         */
        @Test
        void getValidatedPage_returnDefault_whenNull() {
            assertEquals(TEST_DEFAULT_PAGE, pagingPolicy.getValidatedPage(null));
        }

        /**
         * 페이지 검증 - 음수면 기본 값 반환
         */
        @Test
        void getValidatedPage_returnDefault_whenNegative() {
            assertEquals(TEST_DEFAULT_PAGE, pagingPolicy.getValidatedPage(-1));
        }

        /**
         * 페이지 검증 - 0은 그대로 0
         */
        @Test
        void getValidatedPage_zero() {
            assertEquals(0, pagingPolicy.getValidatedPage(0));
        }

        /**
         * 페이지 검증 - 양수면 그대로 반환
         */
        @Test
        void getValidatedPage_positive() {
            assertEquals(5, pagingPolicy.getValidatedPage(5));
        }
    }

    // ========================= 페이지 크기 검증 =========================
    @Nested
    class GetValidatedSizeTest {

        /**
         * 크기 검증 - 0 또는 음수면 기본 값 반환
         */
        @Test
        void getValidatedSize_returnDefault_whenZeroOrNegative() {
            assertEquals(TEST_DEFAULT_SIZE, pagingPolicy.getValidatedSize(0));
            assertEquals(TEST_DEFAULT_SIZE, pagingPolicy.getValidatedSize(-10));
        }

        /**
         * 크기 검증 - 최대 값 초과 시 최대값으로 캡
         */
        @Test
        void getValidatedSize_capsToMax_whenExceedsMax() {
            assertEquals(TEST_MAX_SIZE, pagingPolicy.getValidatedSize(TEST_MAX_SIZE + 50));
        }

        /**
         * 크기 검증 - 유효 범위는 그대로 반환
         */
        @Test
        void getValidatedSize_returnsSame_whenWithinRange() {
            assertEquals(1, pagingPolicy.getValidatedSize(1));
            assertEquals(50, pagingPolicy.getValidatedSize(50));
            assertEquals(TEST_MAX_SIZE, pagingPolicy.getValidatedSize(TEST_MAX_SIZE));
        }
    }

    // ========================= 정렬 필드 검증 =========================
    @Nested
    class SortFieldTest {

        /**
         * 정렬 필드 - 설정 값 그대로 반환
         */
        @Test
        void getTransactionSortField_success() {
            assertEquals(TEST_SORT_FIELD, pagingPolicy.getTransactionSortField());
        }
    }
}