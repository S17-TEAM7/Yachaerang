package com.yachaerang.backend.api.farm.controller;

import com.yachaerang.backend.api.farm.dto.response.FarmResponseDto;
import com.yachaerang.backend.api.farm.dto.resquest.FarmRequestDto;
import com.yachaerang.backend.api.farm.service.FarmService;
import com.yachaerang.backend.global.util.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FarmControllerTest extends RestDocsSupport {

    private final FarmService farmService = mock(FarmService.class);

    @Override
    protected Object initController() {
        return new FarmController(farmService);
    }

    // 공통 응답 필드
    private static final FieldDescriptor[] ENVELOPE_COMMON = new FieldDescriptor[]{
            fieldWithPath("httpStatus").type(STRING).description("HTTP 상태 코드"),
            fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
            fieldWithPath("code").type(STRING).description("응답 코드"),
            fieldWithPath("message").type(STRING).description("응답 메시지")
    };

    private static final FieldDescriptor DATA_OBJECT_DESCRIPTOR =
            fieldWithPath("data").type(OBJECT).description("응답 데이터");

    private static final String AUTH_HEADER = "Authorization";
    private static final String TEST_TOKEN = "Bearer test-access-token";

    /*
    RequestDto 생성 메서드
     */
    private FarmRequestDto.InfoDto createRequestDto() {
        return FarmRequestDto.InfoDto.builder()
                .manpower(5)
                .location("충남 논산")
                .cultivatedArea(1000.0)
                .flatArea(800.0)
                .mainCrop("딸기")
                .build();
    }

    /*
    ResponseDto 생성 메서드
     */
    private FarmResponseDto.InfoDto createResponseDto() {
        return FarmResponseDto.InfoDto.builder()
                .id(1L)
                .manpower(5)
                .location("충남 논산")
                .cultivatedArea(1000.0)
                .flatArea(800.0)
                .mainCrop("딸기")
                .grade("A")
                .farmType("훌륭한 유형")
                .comment("훌륭한 농장입니다")
                .build();
    }

    @Test
    @DisplayName("[POST] /api/v1/farms")
    void saveFarm() throws Exception {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        FarmResponseDto.InfoDto responseDto = createResponseDto();

        given(farmService.saveFarmInfo(any(FarmRequestDto.InfoDto.class)))
                .willReturn(CompletableFuture.completedFuture(responseDto));

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andDo(doc(
                        "save-farm",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(),
                        requestFields(
                                fieldWithPath("manpower").type(NUMBER).description("인력 수").optional(),
                                fieldWithPath("location").type(STRING).description("농장위치").optional(),
                                fieldWithPath("cultivatedArea").type(NUMBER).description("경작 면적 (㎡)").optional(),
                                fieldWithPath("flatArea").type(NUMBER).description("평지 면적 (㎡)").optional(),
                                fieldWithPath("mainCrop").type(STRING).description("주요 작물").optional()
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_OBJECT_DESCRIPTOR)
                                .andWithPrefix("data.",
                                        fieldWithPath("id").type(NUMBER).description("농장 정보 ID"),
                                        fieldWithPath("manpower").type(NUMBER).description("인력 수"),
                                        fieldWithPath("location").type(STRING).description("위치"),
                                        fieldWithPath("cultivatedArea").type(NUMBER).description("경작 면적"),
                                        fieldWithPath("flatArea").type(NUMBER).description("평지 면적"),
                                        fieldWithPath("mainCrop").type(STRING).description("주요 작물"),
                                        fieldWithPath("grade").type(STRING).description("평가 등급"),
                                        fieldWithPath("farmType").type(STRING).description("농장 유형"),
                                        fieldWithPath("comment").type(STRING).description("평가 코멘트")
                                )
                ));

        verify(farmService).saveFarmInfo(any(FarmRequestDto.InfoDto.class));
    }

    @Test
    @DisplayName("[GET] /api/v1/farms")
    void getFarm() throws Exception {
        // given
        FarmResponseDto.InfoDto responseDto = createResponseDto();
        given(farmService.getFarm()).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN))
                .andExpect(status().isOk())
                .andDo(doc(
                        "get-farm",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(),
                        responseFields(
                                fieldWithPath("id").type(NUMBER).description("농장 ID"),
                                fieldWithPath("manpower").type(NUMBER).description("인력 수"),
                                fieldWithPath("location").type(STRING).description("위치"),
                                fieldWithPath("cultivatedArea").type(NUMBER).description("경작 면적"),
                                fieldWithPath("flatArea").type(NUMBER).description("평지 면적"),
                                fieldWithPath("mainCrop").type(STRING).description("주요 작물"),
                                fieldWithPath("grade").type(STRING).description("평가 등급"),
                                fieldWithPath("farmType").type(STRING).description("농장 유형"),
                                fieldWithPath("comment").type(STRING).description("평가 코멘트")
                        )
                ));

        verify(farmService).getFarm();
    }

    @Test
    @DisplayName("[PATCH] /api/v1/farms")
    void updateFarm() throws Exception {
        // given
        FarmRequestDto.InfoDto requestDto = FarmRequestDto.InfoDto.builder()
                .manpower(10)
                .mainCrop("토마토")
                .build();

        FarmResponseDto.InfoDto responseDto = FarmResponseDto.InfoDto.builder()
                .id(1L)
                .manpower(10)
                .location("강원도 화천군")
                .cultivatedArea(1000.0)
                .flatArea(800.0)
                .mainCrop("토마토")
                .grade("A")
                .farmType("훌륭한 유형")
                .comment("훌륭한 농장입니다")
                .build();

        given(farmService.updateFarmInfo(any(FarmRequestDto.InfoDto.class)))
                .willReturn(CompletableFuture.completedFuture(responseDto));

        // when & then
        MvcResult mvcResult = mockMvc.perform(patch("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andDo(doc(
                        "update-farm",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(),
                        requestFields(
                                fieldWithPath("manpower").type(NUMBER).description("인력 수").optional(),
                                fieldWithPath("location").type(STRING).description("위치").optional(),
                                fieldWithPath("cultivatedArea").type(NUMBER).description("경작 면적").optional(),
                                fieldWithPath("flatArea").type(NUMBER).description("평지 면적").optional(),
                                fieldWithPath("mainCrop").type(STRING).description("주요 작물").optional()
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_OBJECT_DESCRIPTOR)
                                .andWithPrefix("data.",
                                        fieldWithPath("id").type(NUMBER).description("농장 ID"),
                                        fieldWithPath("manpower").type(NUMBER).description("인력 수"),
                                        fieldWithPath("location").type(STRING).description("위치"),
                                        fieldWithPath("cultivatedArea").type(NUMBER).description("경작 면적"),
                                        fieldWithPath("flatArea").type(NUMBER).description("평지 면적"),
                                        fieldWithPath("mainCrop").type(STRING).description("주요 작물"),
                                        fieldWithPath("grade").type(STRING).description("평가 등급"),
                                        fieldWithPath("farmType").type(STRING).description("농장 유형"),
                                        fieldWithPath("comment").type(STRING).description("평가 코멘트")
                                )
                ));

        verify(farmService).updateFarmInfo(any(FarmRequestDto.InfoDto.class));
    }
}