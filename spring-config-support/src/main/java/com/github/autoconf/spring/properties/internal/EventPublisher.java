package com.github.autoconf.spring.properties.internal;

import org.springframework.core.io.Resource;

/**
 * 事件发布器
 * Created by lirui on 2015-10-02 14:50.
 */
public interface EventPublisher {
  void onResourceChanged(Resource resource);
}
