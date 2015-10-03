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

import java.util.Arrays;
import java.util.List;

/**
 * 基于远程zookeeper文件的配置
 * Created by lirui on 2015/9/28.
 */
public class RemoteConfig extends ChangeableConfig {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteConfig.class);
  private final String zkPath;
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
        start();
      }
    }
  };

  public RemoteConfig(String name, String zkPath, List<String> paths, CuratorFramework client) {
    super(name);
    this.zkPath = zkPath;
    this.paths = paths;
    this.client = client;
  }

  protected void initZookeeper() {
    client.getConnectionStateListenable().addListener(stateListener);
    if (ZookeeperUtil.exists(client, zkPath, baseWatcher) != null) {
      loadFromZookeeper();
    }
  }

  public void start() {
    initZookeeper();
  }

  protected void loadFromZookeeper() {
    LOG.info("{}, zkPath:{}, order:{}", getName(), zkPath, paths);
    List<String> children = ZookeeperUtil.getChildren(client, zkPath, baseWatcher);
    boolean found = false;
    //按照特定顺序逐个查找配置
    if (children != null && children.size() > 0) {
      LOG.info("zkPath:{}, children:{}", zkPath, children);
      for (String i : paths) {
        if (!children.contains(i))
          continue;
        String path = ZKPaths.makePath(zkPath, i);
        try {
          byte[] content = ZookeeperUtil.getData(client, path, leafWatcher);
          if (content != null && content.length > 0) {
            LOG.info("{}, zkPath:{}", getName(), path);
            reload(content);
            found = true;
            break;
          }
        } catch (Exception e) {
          LOG.error("cannot load {} from zookeeper, zkPath{}", getName(), zkPath, e);
        }
      }
    }
    if (!found) {
      ZookeeperUtil.exists(client, zkPath, baseWatcher);
      LOG.warn("cannot find {} in zookeeper, zkPath{}", getName(), zkPath);
      reload(new byte[0]);
    }
  }

  protected void reload(byte[] content) {
    //只有真正发生变化的时候才触发重新加载
    if (hasChanged(content)) {
      copyOf(content);
      notifyListeners();
    }
  }

  /**
   * 判断新接收到的数据和以前相比是否发生了变化
   *
   * @param now 新数据
   * @return 逐字节对比，不一样就返回true
   */
  protected boolean hasChanged(byte[] now) {
    if (now == null) {
      return true;
    }
    byte[] old = getContent();
    LOG.debug("change detecting\nbefore:\n{}\n\nafter:\n{}\n", ZookeeperUtil.newString(old), ZookeeperUtil.newString(now));
    return !Arrays.equals(now, old);
  }

  public String getZkPath() {
    return zkPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", getName()).add("zkPath", zkPath).toString();
  }
}
