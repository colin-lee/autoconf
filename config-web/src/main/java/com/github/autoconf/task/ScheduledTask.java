package com.github.autoconf.task;

import com.github.autoconf.entity.User;
import com.github.autoconf.service.UserService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务
 * Created by lirui on 2015-10-04 19:33.
 */
@Component
public class ScheduledTask {
  private static final Logger LOG = LoggerFactory.getLogger(ScheduledTask.class);
  private static final DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd");

  @Autowired
  private UserService userService;
  /**
   * 扫描长期不活跃的账户,做锁定处理
   */
  @Scheduled(cron = "0 0 1 * * *")
  public void lockInactiveUser() {
    //查找n天内没有登录的账号
    DateTime dt = new DateTime();
    dt.minusDays(30);
    List<User> users = userService.findNotLoginAfter(DTF.print(dt));
    if (users != null) {
      for (User u : users) {
        LOG.info("{} lastLogin:{}, now LOCK", u.getUsername(), DTF.print(u.getLastLogin().getTime()));
        userService.lock(u.getUsername());
      }
    }
  }
}
