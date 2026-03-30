package com.yachaerang.batch.service.redis;

/**
 * Redis 집계/인덱스 키 형식 상수
 */
public final class RedisAggregationKeys {

    // 집계 Hash Key: v{version}:agg:{type}:{productCode}:{...}
    public static final String AGG_WEEKLY_KEY  = "v%d:agg:weekly:%s:%d:%d";
    public static final String AGG_MONTHLY_KEY = "v%d:agg:monthly:%s:%d:%d";
    public static final String AGG_YEARLY_KEY  = "v%d:agg:yearly:%s:%d";

    // 인덱스 Set Key: v{version}:idx:{type}:{...}
    public static final String IDX_WEEKLY_KEY  = "v%d:idx:weekly:%d:%d";
    public static final String IDX_MONTHLY_KEY = "v%d:idx:monthly:%d:%d";
    public static final String IDX_YEARLY_KEY  = "v%d:idx:yearly:%d";

    // Version 관리 Key
    public static final String ACTIVE_VERSION_KEY = "agg:active_version"; // 현재 live 버전
    public static final String VERSION_SEQ_KEY    = "agg:version_seq";    // 단조 증가 시퀀스

    private RedisAggregationKeys() {}
}
