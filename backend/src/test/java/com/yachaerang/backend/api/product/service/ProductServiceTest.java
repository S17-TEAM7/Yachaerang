package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.ProductResponseDto;
import com.yachaerang.backend.api.product.repository.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Transactional
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductMapper productMapper;
    @InjectMocks ProductService productService;

    @Test
    @DisplayName("아이템 목록 반환 성공")
    void 아이템목록_반환_성공() {
        // given
        List<ProductResponseDto.ItemDto> expectedItems = List.of(
                ProductResponseDto.ItemDto.builder()
                        .itemCode("ITEM001")
                        .itemName("상품1")
                        .build(),
                ProductResponseDto.ItemDto.builder()
                        .itemCode("ITEM002")
                        .itemName("상품2")
                        .build()
        );
        given(productMapper.findAllItemNameAndItemCodes()).willReturn(expectedItems);

        // when
        List<ProductResponseDto.ItemDto> result = productService.getItemNames();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemCode()).isEqualTo("ITEM001");
        assertThat(result.get(0).getItemName()).isEqualTo("상품1");
        assertThat(result.get(1).getItemCode()).isEqualTo("ITEM002");
        assertThat(result.get(1).getItemName()).isEqualTo("상품2");

        verify(productMapper, times(1)).findAllItemNameAndItemCodes();
    }

    @Test
    @DisplayName("빈 목록일 때의 반환 성공")
    void 빈목록일때의_반환_성공() {
        // given
        given(productMapper.findAllItemNameAndItemCodes()).willReturn(Collections.emptyList());

        // when
        List<ProductResponseDto.ItemDto> result = productService.getItemNames();

        // then
        assertThat(result).isEmpty();
        verify(productMapper, times(1)).findAllItemNameAndItemCodes();
    }

    @Test
    @DisplayName("ProductMapper 호출 성공")
    void productMapper_호출_성공() {
        // given
        given(productMapper.findAllItemNameAndItemCodes()).willReturn(Collections.emptyList());

        // when
        productService.getItemNames();

        // then
        verify(productMapper).findAllItemNameAndItemCodes();
        verifyNoMoreInteractions(productMapper);
    }

    @Test
    @DisplayName("제품 목록 하위부류까지 반환 성공")
    void 제품목록_하위부류까지_반환_성공() {
        // given
        String itemCode = "ITEM001";
        List<ProductResponseDto.SubItemDto> expectedProducts = List.of(
                ProductResponseDto.SubItemDto.builder()
                        .name("상품명1")
                        .productCode("PROD001")
                        .itemName("상품1")
                        .itemCode("ITEM001")
                        .kindName("종류1")
                        .kindCode("KIND001")
                        .productRank("A")
                        .rankCode("RANK001")
                        .build(),
                ProductResponseDto.SubItemDto.builder()
                        .name("상품명2")
                        .productCode("PROD002")
                        .itemName("상품1")
                        .itemCode("ITEM001")
                        .kindName("종류1")
                        .kindCode("KIND001")
                        .productRank("B")
                        .rankCode("RANK002")
                        .build()
        );
        given(productMapper.findProductNameByItemCode(itemCode)).willReturn(expectedProducts);

        // when
        List<ProductResponseDto.SubItemDto> result = productService.getProductNames(itemCode);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(product -> {
            assertThat(product.getItemCode()).isEqualTo(itemCode);
        });

        verify(productMapper, times(1)).findProductNameByItemCode(itemCode);
    }

    @Test
    @DisplayName("존재하지 않을 때의 하위부류 빈 목록 반환 성공")
    void 존재하지않을때의_하위부류_빈목록_반환_성공() {
        // given
        String itemCode = "NON_EXISTENT";
        given(productMapper.findProductNameByItemCode(itemCode)).willReturn(Collections.emptyList());

        // when
        List<ProductResponseDto.SubItemDto> result = productService.getProductNames(itemCode);

        // then
        assertThat(result).isEmpty();
        verify(productMapper, times(1)).findProductNameByItemCode(itemCode);
    }

    @Test
    @DisplayName("하위부류DTO 반환 필드 검증")
    void 하위부류DTO_반환_필드_검증() {
        // given
        String itemCode = "ITEM001";
        ProductResponseDto.SubItemDto expectedProduct = ProductResponseDto.SubItemDto.builder()
                .name("테스트상품")
                .productCode("PROD001")
                .itemName("아이템1")
                .itemCode("ITEM001")
                .kindName("종류A")
                .kindCode("KIND001")
                .productRank("특")
                .rankCode("RANK001")
                .build();

        given(productMapper.findProductNameByItemCode(itemCode))
                .willReturn(List.of(expectedProduct));

        // when
        List<ProductResponseDto.SubItemDto> result = productService.getProductNames(itemCode);

        // then
        assertThat(result).hasSize(1);
        ProductResponseDto.SubItemDto actual = result.get(0);

        assertThat(actual.getName()).isEqualTo("테스트상품");
        assertThat(actual.getProductCode()).isEqualTo("PROD001");
        assertThat(actual.getItemName()).isEqualTo("아이템1");
        assertThat(actual.getItemCode()).isEqualTo("ITEM001");
        assertThat(actual.getKindName()).isEqualTo("종류A");
        assertThat(actual.getKindCode()).isEqualTo("KIND001");
        assertThat(actual.getProductRank()).isEqualTo("특");
        assertThat(actual.getRankCode()).isEqualTo("RANK001");
    }

    @Test
    @DisplayName("productMapper 호출 시 파라미터 전달 검증")
    void setProductMapper_호출시_파라미터_전달_검증() {
        // given
        String itemCode = "ITEM001";
        given(productMapper.findProductNameByItemCode(anyString()))
                .willReturn(Collections.emptyList());

        // when
        productService.getProductNames(itemCode);

        // then
        verify(productMapper).findProductNameByItemCode(eq("ITEM001"));
    }

    // ─── 캐시 레이어 테스트 ────────────────────────────────────────────────────
    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = CacheLayerTest.TestConfig.class)
    class CacheLayerTest {

        @Configuration
        @EnableCaching
        static class TestConfig {

            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("product:itemNames", "product:subItems");
            }

            @Bean
            public ProductMapper productMapper() {
                return Mockito.mock(ProductMapper.class);
            }

            @Bean
            public ProductService productService(ProductMapper productMapper) {
                return new ProductService(productMapper);
            }
        }

        @Autowired
        ProductMapper productMapper;

        @Autowired
        ProductService productService;

        @Autowired
        CacheManager cacheManager;

        @BeforeEach
        void resetMocksAndCaches() {
            Mockito.reset(productMapper);
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        }

        @Test
        @DisplayName("getItemNames: 두 번 호출해도 매퍼 1회만 호출 (캐시 히트)")
        void getItemNames_두번호출_캐시히트() {
            List<ProductResponseDto.ItemDto> data = List.of(
                    ProductResponseDto.ItemDto.builder().itemCode("ITEM001").itemName("과일").build()
            );
            given(productMapper.findAllItemNameAndItemCodes()).willReturn(data);

            productService.getItemNames();
            productService.getItemNames();

            verify(productMapper, times(1)).findAllItemNameAndItemCodes();
        }

        @Test
        @DisplayName("getProductNames: 동일 itemCode 반복 호출 시 매퍼 1회만 호출 (캐시 히트)")
        void getProductNames_동일itemCode_캐시히트() {
            String itemCode = "ITEM001";
            List<ProductResponseDto.SubItemDto> data = List.of(
                    ProductResponseDto.SubItemDto.builder()
                            .productCode("PROD001").name("사과").itemCode(itemCode)
                            .itemName("과일").kindName("사과").kindCode("K001")
                            .productRank("특").rankCode("R001").build()
            );
            given(productMapper.findProductNameByItemCode(itemCode)).willReturn(data);

            productService.getProductNames(itemCode);
            productService.getProductNames(itemCode);

            verify(productMapper, times(1)).findProductNameByItemCode(itemCode);
        }

        @Test
        @DisplayName("getProductNames: 다른 itemCode는 별도 캐시 키 → 각각 매퍼 호출")
        void getProductNames_다른itemCode_별도캐시키() {
            String itemCode1 = "ITEM001";
            String itemCode2 = "ITEM002";

            given(productMapper.findProductNameByItemCode(itemCode1)).willReturn(Collections.emptyList());
            given(productMapper.findProductNameByItemCode(itemCode2)).willReturn(Collections.emptyList());

            productService.getProductNames(itemCode1);
            productService.getProductNames(itemCode2);

            verify(productMapper, times(1)).findProductNameByItemCode(itemCode1);
            verify(productMapper, times(1)).findProductNameByItemCode(itemCode2);
        }

        @Test
        @DisplayName("getItemNames 캐시와 getProductNames 캐시는 독립적으로 동작")
        void itemNames캐시와_subItems캐시_독립동작() {
            given(productMapper.findAllItemNameAndItemCodes()).willReturn(Collections.emptyList());
            given(productMapper.findProductNameByItemCode(anyString())).willReturn(Collections.emptyList());

            productService.getItemNames();
            productService.getItemNames();       // 캐시 히트
            productService.getProductNames("ITEM001");
            productService.getProductNames("ITEM001"); // 캐시 히트

            verify(productMapper, times(1)).findAllItemNameAndItemCodes();
            verify(productMapper, times(1)).findProductNameByItemCode("ITEM001");
        }
    }
}
