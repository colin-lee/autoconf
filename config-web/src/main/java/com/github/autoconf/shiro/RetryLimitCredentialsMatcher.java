package com.github.autoconf.shiro;

import com.github.autoconf.service.CacheService;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryLimitCredentialsMatcher extends HashedCredentialsMatcher {
  @Autowired
  private CacheService cacheService;

  @Override
  public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
    String username = (String) token.getPrincipal();
    //retry count + 1
    AtomicInteger retryCount = cacheService.getRetryLimit(username);
    if (retryCount.incrementAndGet() > 5) {
      //if retry count > 5 throw
      throw new ExcessiveAttemptsException();
    }

    boolean matches = super.doCredentialsMatch(token, info);
    if (matches) {
      //clear retry count
      cacheService.clearRetryCache(username);
    }
    return matches;
  }
}
