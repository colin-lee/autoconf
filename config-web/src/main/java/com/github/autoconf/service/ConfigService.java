package com.github.autoconf.service;

import com.github.autoconf.entity.Config;
import com.github.autoconf.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dao访问层
 *
 * Created by lirui on 2015/1/10.
 */
@Service
public class ConfigService {
  private static final String NAME = "ConfigCache";
  @Autowired
  private ConfigMapper mapper;

  @CacheEvict(value = NAME, allEntries = true)
  public void save(Config config) {
    if (config.isNew()) {
      mapper.insertAndGetId(config);
    } else {
      config.incVersion();
      mapper.update(config);
    }
  }

  public Config findById(Long id) {
    return mapper.findById(id);
  }

  @Cacheable(value = NAME, unless = "#result == null")
  public List<Config> findAll() {
    return mapper.findAll();
  }

  @CacheEvict(value = NAME, allEntries = true)
  public void clearCache() {
  }

  public void delete(Config config) {
    if (config != null) {
      config.setContent("#deletedBy " + config.getEditor()); //标识为被删除
      config.incVersion();
      mapper.deleteById(config.getId());
    }
  }

  public void updatePath(String path, Long id) {
    mapper.updatePath(path, id);
  }
}

