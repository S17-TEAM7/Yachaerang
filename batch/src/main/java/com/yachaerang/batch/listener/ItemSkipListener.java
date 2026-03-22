package com.yachaerang.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Read 중 Skip 발생: exceptionClass={}, message={}",
                t.getClass().getName(), t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("Process 중 Skip 발생: item={}, exceptionClass={}, message={}",
                item, t.getClass().getName(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("Write 중 Skip 발생: item={}, exceptionClass={}, message={}",
                item, t.getClass().getName(), t.getMessage());
    }
}
