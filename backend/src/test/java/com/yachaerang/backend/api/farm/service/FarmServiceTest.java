package com.yachaerang.backend.api.farm.service;

import com.yachaerang.backend.api.farm.dto.response.FarmResponseDto;
import com.yachaerang.backend.api.farm.dto.resquest.FarmRequestDto;
import com.yachaerang.backend.api.farm.entity.Farm;
import com.yachaerang.backend.api.farm.repository.FarmMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock private FarmMapper farmMapper;
    @Mock private FarmEvaluationService farmEvaluationService;
    @Mock private FarmAsyncService farmAsyncService;
    @Mock private AuthenticatedMemberProvider authenticatedMemberProvider;
    @InjectMocks private FarmService farmService;

    private static final Long MEMBER_ID = 1L;
    private static final Long FARM_ID = 100L;

    /*
    DTO 생성 메서드
     */
    private FarmRequestDto.InfoDto createRequestDto() {
        return FarmRequestDto.InfoDto.builder()
                .manpower(5)
                .location("경기도 화성시")
                .cultivatedArea(1000.0)
                .flatArea(800.0)
                .mainCrop("딸기")
                .build();
    }

    private Farm createFarm() {
        return createFarmWithId(FARM_ID);
    }

    private Farm createFarmWithId(Long id) {
        return Farm.builder()
                .id(id)
                .memberId(MEMBER_ID)
                .manpower(5)
                .location("경기도 화성시")
                .cultivatedArea(1000.0)
                .flatArea(800.0)
                .mainCrop("딸기")
                .grade("A")
                .comment("좋은 농장이에요!")
                .build();
    }

    @BeforeEach
    void setUp() {
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(MEMBER_ID);
    }

    @Test
    @DisplayName("농장 정보 저장 성공")
    void 농장정보_저장_성공() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        FarmResponseDto.EvaluateDto evaluationDto = new FarmResponseDto.EvaluateDto("A", "좋은 유형", "좋은 농장입니다");
        Farm savedFarm = createFarm();

        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.completedFuture(evaluationDto));
        when(farmAsyncService.saveFarmWithEvaluation(
                eq(MEMBER_ID), eq(requestDto), eq("A"), eq("좋은 유형"), eq("좋은 농장입니다")))
                .thenReturn(savedFarm);

        // when
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.saveFarmInfo(requestDto);
        FarmResponseDto.InfoDto responseDto = result.join();

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(FARM_ID);
        verify(farmAsyncService).saveFarmWithEvaluation(
                eq(MEMBER_ID), eq(requestDto), eq("A"), eq("좋은 유형"), eq("좋은 농장입니다"));
    }

    @Test
    @DisplayName("농장 정보 저장 실패 - AI 평가 실패")
    void 농장정보_저장_실패_AI평가_실패() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();

        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AI 서비스 오류")));

        // when & then
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.saveFarmInfo(requestDto);

        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_EVALUATION_FAILED);
                });
    }

    @Test
    @DisplayName("농장 정보 저장 실패 - DB 저장 실패")
    void 농장정보_저장_실패_DB저장_실패() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        FarmResponseDto.EvaluateDto evaluationDto = new FarmResponseDto.EvaluateDto("A","좋은 유형", "좋은 농장입니다");

        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.completedFuture(evaluationDto));
        when(farmAsyncService.saveFarmWithEvaluation(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB 오류"));

        // when & then
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.saveFarmInfo(requestDto);

        assertThatThrownBy(result::join)
                .hasCauseInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("농장 정보 조회 성공")
    void 농장정보_조회_성공() {
        // given
        Farm farm = createFarm();
        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(farm);

        // when
        FarmResponseDto.InfoDto result = farmService.getFarm();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(FARM_ID);
        verify(farmMapper).findByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("농장 정보 없을 시 null 반환")
    void 농장정보_없을시_null_반환() {
        // given
        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(null);

        // when
        FarmResponseDto.InfoDto result = farmService.getFarm();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("농장 정보 수정 성공")
    void 농장정보_수정_성공() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        Farm existingFarm = createFarm();
        Farm updatedFarm = createFarm();
        FarmResponseDto.EvaluateDto evaluationDto = new FarmResponseDto.EvaluateDto("A", "좋은 유형", "매우 좋은 농장이네요!");

        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(existingFarm);
        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.completedFuture(evaluationDto));
        when(farmAsyncService.updateFarmWithEvaluation(
                eq(MEMBER_ID), eq(requestDto), eq("A"), eq("좋은 유형"), eq("매우 좋은 농장이네요!")))
                .thenReturn(updatedFarm);

        // when
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.updateFarmInfo(requestDto);
        FarmResponseDto.InfoDto responseDto = result.join();

        // then
        assertThat(responseDto).isNotNull();
        verify(farmAsyncService).updateFarmWithEvaluation(
                eq(MEMBER_ID), eq(requestDto), eq("A"), eq("좋은 유형"), eq("매우 좋은 농장이네요!"));
    }

    @Test
    @DisplayName("저장된 농장이 없을 시 농장 정보 수정 실패")
    void 저장된_농장이_없을시_농장정보_수정_실패() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> farmService.updateFarmInfo(requestDto))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FARM_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("농장 정보 저장 실패 - 이미 존재하는 농장")
    void 농장정보_저장_실패_이미존재하는_농장() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        FarmResponseDto.EvaluateDto evaluationDto = new FarmResponseDto.EvaluateDto("A", "좋은 유형", "좋은 농장입니다");
        Farm existingFarm = createFarm();

        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.completedFuture(evaluationDto));
        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(existingFarm);

        // when
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.saveFarmInfo(requestDto);

        // then
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FARM_ALREADY_EXISTS);
                });
    }

    @Test
    @DisplayName("농장 정보 수정 실패 - AI 평가 실패")
    void 농장정보_수정_실패_AI평가_실패() {
        // given
        FarmRequestDto.InfoDto requestDto = createRequestDto();
        Farm existingFarm = createFarm();

        when(farmMapper.findByMemberId(MEMBER_ID)).thenReturn(existingFarm);
        when(farmEvaluationService.generateGradeAndComment(requestDto))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AI 서비스 오류")));

        // when
        CompletableFuture<FarmResponseDto.InfoDto> result = farmService.updateFarmInfo(requestDto);

        // then
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    GeneralException exception = (GeneralException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FARM_UPDATE_FAILED);
                });
    }
}