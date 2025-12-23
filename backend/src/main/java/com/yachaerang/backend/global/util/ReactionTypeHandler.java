package com.yachaerang.backend.global.util;

import com.yachaerang.backend.api.common.ReactionType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes({ReactionType.class})
public class ReactionTypeHandler extends CodeEnumTypeHandler<ReactionType> {

    public ReactionTypeHandler() {
        super(ReactionType.class);
    }
}
