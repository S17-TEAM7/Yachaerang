package com.yachaerang.batch.service.redis;

/**
 * Redis 집계/인덱스 키 형식 상수.
 * RedisAggregationWriter 와 RedisAggregationReader 가 공유한다.
 */
public final class RedisAggregationKeys {

    public static final String AGG_WEEKLY_KEY  = "agg:weekly:%s:%d:%d";
    public static final String AGG_MONTHLY_KEY = "agg:monthly:%s:%d:%d";
    public static final String AGG_YEARLY_KEY  = "agg:yearly:%s:%d";

    public static final String IDX_WEEKLY_KEY  = "idx:weekly:%d:%d";
    public static final String IDX_MONTHLY_KEY = "idx:monthly:%d:%d";
    public static final String IDX_YEARLY_KEY  = "idx:yearly:%d";

    private RedisAggregationKeys() {}
}
