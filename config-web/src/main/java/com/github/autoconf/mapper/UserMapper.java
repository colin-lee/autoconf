package com.github.autoconf.mapper;

import com.github.autoconf.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户相关操作
 * Created by lirui on 2015-10-04 19:11.
 */
@Repository
public interface UserMapper {
  @Select("SELECT * FROM user WHERE username=#{username}")
  User findByUserName(@Param("username") String username);

  @Insert("INSERT user SET username=#{username}, password=#{password}, salt=#{salt}, create_time=NOW()")
  void create(User user);

  @Update("UPDATE user SET roles=#{roles}, permissions=#{permissions}, locked=#{locked} WHERE username=#{username}")
  void updateAuthentication(User user);

  @Insert("UPDATE user SET password=#{password}, salt=#{salt} WHERE username=#{username}")
  void updatePassword(User user);

  @Select("SELECT * FROM user")
  List<User> findAll();

  @Update("UPDATE user SET locked=1 WHERE username=#{username}")
  void lock(@Param("username") String username);

  @Update("UPDATE user SET last_login=NOW() WHERE username=#{username}")
  void updateLoginTime(@Param("username") String username);

  @Select("SELECT * FROM user WHERE last_login<#{day}")
  List<User> findNotLoginAfter(@Param("day") String day);
}
