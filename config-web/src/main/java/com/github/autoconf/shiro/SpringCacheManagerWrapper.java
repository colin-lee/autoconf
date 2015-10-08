package com.github.autoconf.shiro;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.Collection;
import java.util.Set;

public class SpringCacheManagerWrapper implements CacheManager {
  private org.springframework.cache.CacheManager cacheManager;

  /**
   * 设置spring cache manager
   *
   * @param cacheManager cache类信息
   */
  public void setCacheManager(org.springframework.cache.CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <K, V> Cache<K, V> getCache(String name) throws CacheException {
    org.springframework.cache.Cache springCache = cacheManager.getCache(name);
    return new SpringCacheWrapper(springCache);
  }

  static class SpringCacheWrapper implements Cache {
    private org.springframework.cache.Cache springCache;

    SpringCacheWrapper(org.springframework.cache.Cache springCache) {
      this.springCache = springCache;
    }

    @Override
    public Object get(Object key) throws CacheException {
      Object value = springCache.get(key);
      if (value instanceof SimpleValueWrapper) {
        return ((SimpleValueWrapper) value).get();
      }
      return value;
    }

    @Override
    public Object put(Object key, Object value) throws CacheException {
      springCache.put(key, value);
      return value;
    }

    @Override
    public Object remove(Object key) throws CacheException {
      springCache.evict(key);
      return null;
    }

    @Override
    public void clear() throws CacheException {
      springCache.clear();
    }

    @Override
    public int size() {
      if (springCache instanceof GuavaCache) {
        GuavaCache cache = (GuavaCache) springCache;
        return (int) cache.getNativeCache().size();
      }
      throw new UnsupportedOperationException("invoke spring cache abstract size method not supported");
    }

    @Override
    public Set keys() {
      if (springCache instanceof GuavaCache) {
        GuavaCache cache = (GuavaCache) springCache;
        return cache.getNativeCache().asMap().keySet();
      }
      throw new UnsupportedOperationException("invoke spring cache abstract keys method not supported");
    }

    @Override
    public Collection values() {
      if (springCache instanceof GuavaCache) {
        GuavaCache cache = (GuavaCache) springCache;
        return cache.getNativeCache().asMap().values();
      }
      throw new UnsupportedOperationException("invoke spring cache abstract values method not supported");
    }
  }
}
