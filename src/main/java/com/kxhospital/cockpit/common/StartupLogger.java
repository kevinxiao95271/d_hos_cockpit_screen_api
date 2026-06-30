package com.kxhospital.cockpit.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupLogger {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("========================================");
        log.info("  大屏驾驶舱 DS 模块已启动");
        log.info("  端口: 8060");
        log.info("  接口: /ds/qc/*  /ds/stat/*");
        log.info("========================================");
    }
}
