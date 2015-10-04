package com.github.autoconf.service;

import com.github.autoconf.entity.User;
import com.github.autoconf.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 用户相关服务
 * Created by lirui on 2015-10-04 18:17.
 */
@Service
public class UserService {
  @Autowired
  private UserMapper mapper;

  @Cacheable
  public User findByUsername(String username) {
    return mapper.findByUserName(username);
  }
}
