package com.github.autoconf.impl;

import com.github.autoconf.base.ChangeableConfig;
import com.github.autoconf.helper.ZookeeperUtil;
import com.google.common.base.MoreObjects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于远程zookeeper文件的配置
 * Created by lirui on 2015/9/28.
 */
public class RemoteConfig extends ChangeableConfig {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteConfig.class);
  private final String path;
  private final List<String> paths;
  private final CuratorFramework client;
  private final Watcher baseWatcher = new Watcher() {
    public void process(WatchedEvent event) {
      Event.EventType t = event.getType();
      String p = event.getPath();
      switch (t) {
        case NodeCreated:
        case NodeChildrenChanged:
          loadFromZookeeper();
          break;
        case NodeDeleted:
          client.clearWatcherReferences(this);
          loadFromZookeeper();
          break;
        default:
          LOG.warn("skip {}, {}", t, p);
      }
    }
  };
  private final Watcher leafWatcher = new Watcher() {
    @Override
    public void process(WatchedEvent event) {
      Event.EventType t = event.getType();
      String p = event.getPath();
      switch (t) {
        case NodeDataChanged:
          loadFromZookeeper();
          break;
        case NodeDeleted:
          client.clearWatcherReferences(this);
          loadFromZookeeper();
          break;
        default:
          LOG.warn("skip {}, {}", t, p);
      }
    }
  };
  private final ConnectionStateListener stateListener = new ConnectionStateListener() {
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
      if (newState.equals(ConnectionState.RECONNECTED)) {
        initZookeeper();
      }
    }
  };

  public RemoteConfig(String name, String path, List<String> paths, CuratorFramework client) {
    super(name);
    this.path = path;
    this.paths = paths;
    this.client = client;
  }

  protected void initZookeeper() {
    try {
      client.getConnectionStateListenable().addListener(stateListener);
      if (!client.getZookeeperClient().isConnected()) {
        client.blockUntilConnected(10, TimeUnit.SECONDS);
      }
      if (ZookeeperUtil.exists(client, path, baseWatcher) != null) {
        loadFromZookeeper();
      }
    } catch (InterruptedException e) {
      LOG.error("cannot init '{}', path:{}", getName(), path, e);
    }
  }

  public void start() {
    initZookeeper();
  }

  protected void loadFromZookeeper() {
    LOG.info("{}, path:{}, order:{}", getName(), path, paths);
    List<String> children = ZookeeperUtil.getChildren(client, path, baseWatcher);
    boolean found = false;
    //按照特定顺序逐个查找配置
    if (children != null && children.size() > 0) {
      LOG.info("path:{}, children:{}", path, children);
      for (String i : paths) {
        if (!children.contains(i))
          continue;
        String p = ZKPaths.makePath(path, i);
        try {
          byte[] content = ZookeeperUtil.getData(client, p, leafWatcher);
          if (content != null && content.length > 0) {
            LOG.info("{}, path:{}", getName(), p);
            reload(content);
            found = true;
            break;
          }
        } catch (Exception e) {
          LOG.error("cannot load {} from zookeeper, path{}", getName(), path, e);
        }
      }
    }
    if (!found) {
      ZookeeperUtil.exists(client, path, baseWatcher);
      LOG.warn("cannot find {} in zookeeper, path{}", getName(), path);
      reload(new byte[0]);
    }
  }

  protected void reload(byte[] content) {
    //只有真正发生变化的时候才触发重新加载
    if (isChanged(content)) {
      copyOf(content);
      notifyListeners();
    }
  }

  public String getPath() {
    return path;
  }

  public CuratorFramework getClient() {
    return client;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", getName()).add("path", path).toString();
  }
}
