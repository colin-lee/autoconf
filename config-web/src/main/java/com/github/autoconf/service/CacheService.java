package com.github.autoconf.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * cache服务
 * Created by lirui on 2015-10-07 16:55.
 */
@Service
public class CacheService {
  @Cacheable(value = "RetryLimitCache")
  public AtomicInteger getRetryLimit(String username) {
    return new AtomicInteger(0);
  }

  @CacheEvict(value = "RetryLimitCache")
  public void clearRetryCache(String username) {
  }
}
