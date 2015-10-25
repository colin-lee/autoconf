package com.github.autoconf.impl;

import com.github.autoconf.api.IFileListener;
import com.github.autoconf.watcher.FileUpdateWatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 基于远程zookeeper文件的配置,启用本地文件缓存.
 * <pre>
 * 1.启动首先查找本地配置,如果有的话,会先使用本地配置.
 * 2.同时启动异步线程检查远程zookeeper的配置.
 * 3.远程zookeeper改动内容会同步些往本地文件缓存.
 * </pre>
 * Created by lirui on 2015/9/30.
 */
public class RemoteConfigWithCache extends RemoteConfig {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteConfigWithCache.class);
  private final File cacheFile;
  /**
   * 延迟加载远程配置初始值,避免加载配置影响启动
   */
  private long delaySeconds = 20;
  private boolean loadedFromZookeeper = false;

  public RemoteConfigWithCache(String name, String basePath, List<String> paths, CuratorFramework client, File cacheFile) {
    super(name, basePath, paths, client);
    this.cacheFile = cacheFile;
  }

  public File getCacheFile() {
    return cacheFile;
  }

  public void setDelaySeconds(long delaySeconds) {
    this.delaySeconds = delaySeconds;
  }

  @Override
  public void start() {
    Set<RemoteConfig> checking = Sets.newConcurrentHashSet();
    //有本地配置就先从本地加载
    if (cacheFile.exists()) {
      try {
        copyOf(Files.toByteArray(cacheFile));
        //异步检查zookeeper中配置
        checking.add(this);
      } catch (IOException e) {
        LOG.error("cannot read {}", cacheFile);
        initZookeeper();
      }
    } else {
      //本地没有则直接从zookeeper加载
      initZookeeper();
    }
    //注册本地配置变更通知回调
    FileUpdateWatcher.getInstance().watch(cacheFile.toPath(), new IFileListener() {
      @Override
      public void changed(Path path, byte[] content) {
        LOG.info("local change: {}", path);
        refresh(content);
      }
    });
    //延迟加载zookeeper上的配置,避免服务启动过慢
    asyncCheckZookeeper(checking);
  }

  private void asyncCheckZookeeper(final Set<RemoteConfig> asyncCheck) {
    Thread zkThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(delaySeconds * 1000);
        } catch (InterruptedException ignored) {
          return;
        }
        for (RemoteConfig i : asyncCheck) {
          try {
            i.initZookeeper();
          } catch (Exception e) {
            LOG.error("cannot init {}, cacheFile={}", i.getName(), getCacheFile(), e);
          }
        }
        asyncCheck.clear();
        LOG.info("async load from zookeeper, DONE");
      }
    }, "asyncLoadFromZookeeper");
    zkThread.setDaemon(true);
    zkThread.start();
  }

  private void refresh(byte[] content) {
    if (isChanged(content)) {
      copyOf(content);
      notifyListeners();
      try {
        //已经加载过,就不要再通过本地文件修改通知再加载1次了
        FileUpdateWatcher.getInstance().mask(cacheFile.toPath());
        Files.write(content, cacheFile);
      } catch (IOException e) {
        LOG.error("cannot write {}", cacheFile);
      }
    }
  }

  @Override
  protected void reload(byte[] content) {
    //避免首次启动,远程配置不存在反而覆盖了本地配置
    if ((content == null || content.length == 0) && !loadedFromZookeeper) {
      LOG.warn("{} deleted, wont clean local for safety", getPath());
      return;
    }
    loadedFromZookeeper = true;
    refresh(content);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", getName()).add("cacheFile", cacheFile).add("zkPath", getPath()).toString();
  }
}
