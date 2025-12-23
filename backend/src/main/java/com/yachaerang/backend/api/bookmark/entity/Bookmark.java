package com.yachaerang.backend.api.bookmark.entity;

import com.yachaerang.backend.api.common.BaseEntity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

    private Long bookmarkId;
    private Long memberId;
    private Long articleId;
}
