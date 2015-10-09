package com.github.autoconf.mapper;

import com.github.autoconf.entity.ConfigHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问config对象
 *
 * Created by lirui on 2015/1/10.
 */
public interface ConfigHistoryMapper {
  @Select("SELECT * FROM config_history ORDER BY id DESC LIMIT 500")
  List<ConfigHistory> findRecent();

  @Select("SELECT * FROM config_history WHERE editor=#{editor} ORDER BY id DESC LIMIT 500")
  List<ConfigHistory> findByEditor(@Param("editor") String editor);

  @Select("SELECT * FROM config_history WHERE config_id=#{configId} ORDER BY id DESC LIMIT 500")
  List<ConfigHistory> findByConfigId(@Param("configId") long configId);

  @Select("SELECT * FROM config_history WHERE config_id=#{configId} AND editor=#{editor} ORDER BY id DESC LIMIT 500")
  List<ConfigHistory> findByConfigIdAndEditor(@Param("configId") long configId, @Param("editor") String editor);

  @Select("SELECT * FROM config_history")
  List<ConfigHistory> findAll();

  @Insert("INSERT config_history SET config_id=#{configId}, version=#{version}, editor=#{editor}, name=#{name}, profile=#{profile}, path=#{path}, content=#{content}")
  void insert(ConfigHistory history);

  @Select("SELECT * FROM config_history WHERE id=#{id}")
  ConfigHistory findById(@Param("id") Long id);
}
