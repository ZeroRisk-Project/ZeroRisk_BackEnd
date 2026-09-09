package com.zerorisk.project.domain.stock.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockCodeTypeTest {

    @DisplayName("코드 마지막 자리가 0이면 보통주")
    @Test
    void 코드_마지막_자리가_0이면_보통주() {
        assertThat(StockCodeType.isPreferred("005930")).isFalse();
    }

    @DisplayName("코드 마지막 자리가 0이 아니면 우선주")
    @Test
    void 코드_마지막_자리가_0이_아니면_우선주() {
        assertThat(StockCodeType.isPreferred("005935")).isTrue();
        assertThat(StockCodeType.isPreferred("005371")).isTrue();
    }

    @DisplayName("코드가 없으면 보통주로 취급한다")
    @Test
    void 코드가_없으면_보통주로_취급한다() {
        assertThat(StockCodeType.isPreferred(null)).isFalse();
        assertThat(StockCodeType.isPreferred("")).isFalse();
    }
}
