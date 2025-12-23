package com.yachaerang.backend.api.common;

import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.global.util.LogUtil;

public enum ReactionType implements CodeEnum {
    HELPFUL("HELPFUL"), // 유익해요
    GOOD("GOOD"), // 좋아요
    SURPRISED("SURPRISED"), // 놀랐어요
    SAD("SAD"), // 슬퍼요
    BUMMER("BUMMER"); // 아쉬워요

    private final String code;

    ReactionType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /*
    DB의 VARCHAR -> Enum 클래스로 변환
    */
    public static ReactionType fromCode(String code) {

        for (ReactionType reactionType : ReactionType.values()) {
            if (reactionType.getCode().equals(code)) {
                return reactionType;
            }
        }
        LogUtil.warn("Invalid reaction type code: {}", code);
        throw GeneralException.of(ErrorCode.ENUM_MAPPED_FAILED);
    }
}
