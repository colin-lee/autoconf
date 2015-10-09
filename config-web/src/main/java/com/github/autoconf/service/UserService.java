package com.github.autoconf.service;

import com.github.autoconf.entity.User;
import com.github.autoconf.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户相关服务
 * Created by lirui on 2015-10-04 18:17.
 */
@Service
public class UserService {
  @Autowired
  private UserMapper mapper;

  @Cacheable(value = "UserCache")
  public User findByUsername(String username) {
    return mapper.findByUserName(username);
  }

  public void create(User user) {
    mapper.create(user);
  }

  @CacheEvict(value = "UserCache", key = "#user.username")
  public void updateAuthentication(User user) {
    mapper.updateAuthentication(user);
  }

  @CacheEvict(value = "UserCache", key = "#user.username")
  public void updatePassword(User user) {
    mapper.updatePassword(user);
  }

  public List<User> findAll() {
    return mapper.findAll();
  }

  @CacheEvict(value = "UserCache")
  public void lock(String username) {
    mapper.lock(username);
  }

  @CacheEvict(value = "UserCache")
  public void updateLoginTime(String username) {
    mapper.updateLoginTime(username);
  }

  public List<User> findNotLoginAfter(String day) {
    return mapper.findNotLoginAfter(day);
  }
}
