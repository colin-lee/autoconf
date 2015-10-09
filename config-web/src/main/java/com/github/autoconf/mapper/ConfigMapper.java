package com.github.autoconf.mapper;

import com.github.autoconf.entity.Config;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 访问config对象
 *
 * Created by lirui on 2015/1/10.
 */
public interface ConfigMapper {
  @Select("SELECT * FROM config WHERE name=#{name} AND profile=#{profile}")
  Config findByNameAndProfile(@Param("name") String name, @Param("profile") String profile);

  @Select("SELECT * FROM config WHERE id=#{id}")
  Config findById(@Param("id") Long id);

  @Select("SELECT * FROM config")
  List<Config> findAll();

  @Delete("DELETE FROM config WHERE id=#{id}")
  void deleteById(@Param("id") Long id);

  @Insert("INSERT config SET editor=#{editor}, name=#{name}, profile=#{profile}, content=#{content}")
  @Options(useGeneratedKeys = true)
  void insertAndGetId(Config config);

  @Update("UPDATE config SET editor=#{editor}, version=#{version}, content=#{content} WHERE id=#{id}")
  void update(Config config);

  @Update("UPDATE config SET path=#{path} WHERE id=#{id}")
  void updatePath(@Param("path") String path, @Param("id") Long id);
}
