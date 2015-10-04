package com.github.autoconf.mapper;

import com.github.autoconf.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户相关操作
 * Created by lirui on 2015-10-04 19:11.
 */
public interface UserMapper {
  @Select("SELECT * FROM user WHERE username=#{username}")
  User findByUserName(@Param("username") String username);
}
