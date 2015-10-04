package com.github.autoconf.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 * Created by lirui on 2015-10-04 19:33.
 */
@Component
public class ScheduledTask {

  /**
   * 扫描长期不活跃的账户,做锁定处理
   */
  @Scheduled(cron = "")
  public void lockInactiveUser() {

  }
}
