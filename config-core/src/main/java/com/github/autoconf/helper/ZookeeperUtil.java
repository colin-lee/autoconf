package com.github.autoconf.helper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.Charset;
import java.util.List;

/**
 * zookeeper工具类
 * Created by lirui on 2015-09-29 11:05.
 */
public class ZookeeperUtil {
  public static final Charset UTF8 = Charset.forName("UTF-8");
  public static final Charset GBK = Charset.forName("GBK");

  private ZookeeperUtil() {
  }

  public static String newString(byte[] data) {
    return newString(data, UTF8);
  }

  public static String newString(byte[] data, Charset charset) {
    if (data == null) {
      return null;
    }
    return new String(data, charset);
  }

  public static byte[] newBytes(String s) {
    if (s == null) {
      return null;
    }
    return s.getBytes(UTF8);
  }

  public static byte[] newBytes(String s, Charset charset) {
    if (s == null) {
      return null;
    }
    return s.getBytes(charset);
  }

  public static String ensure(CuratorFramework client, String path) {
    try {
      client.create().creatingParentContainersIfNeeded().forPath(path);
    } catch (Exception e) {
      throw new RuntimeException("ensure(" + path + ")", e);
    }
    return path;
  }

  public static Stat exists(CuratorFramework client, String path) {
    try {
      return client.checkExists().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("exists(" + path + ")", e);
    }
    return null;
  }

  public static Stat exists(CuratorFramework client, String path, Watcher watcher) {
    try {
      return client.checkExists().usingWatcher(watcher).forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("exists(" + path + ")", e);
    }
    return null;
  }

  public static void create(CuratorFramework client, String path) {
    try {
      client.create().creatingParentsIfNeeded().forPath(path);
    } catch (Exception e) {
      throw new RuntimeException("create(" + path + ")", e);
    }
  }

  public static void create(CuratorFramework client, String path, byte[] payload) {
    try {
      client.create().creatingParentsIfNeeded().forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException("create(" + path + ")", e);
    }
  }

  public static void create(CuratorFramework client, String path, byte[] payload, CreateMode mode) {
    try {
      client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException("create(" + path + ")", e);
    }
  }

  public static void create(CuratorFramework client, String path, byte[] payload, CreateMode mode, List<ACL> aclList) {
    try {
      client.create().creatingParentsIfNeeded().withMode(mode).withACL(aclList).forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException("create(" + path + ")", e);
    }
  }

  public static void delete(CuratorFramework client, String path) {
    try {
      client.delete().deletingChildrenIfNeeded().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("delete(" + path + ")", e);
    }
  }

  public static void guaranteedDelete(CuratorFramework client, String path) {
    try {
      client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("guaranteedDelete(" + path + ")", e);
    }
  }

  public static byte[] getData(CuratorFramework client, String path) {
    try {
      return client.getData().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("getData(" + path + ")", e);
    }
    return null;
  }

  public static byte[] getData(CuratorFramework client, String path, Watcher watcher) {
    try {
      return client.getData().usingWatcher(watcher).forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("getData(" + path + ")", e);
    }
    return null;
  }

  public static List<String> getChildren(CuratorFramework client, String path) {
    try {
      return client.getChildren().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("getChildren(" + path + ")", e);
    }
    return null;
  }

  public static List<String> getChildren(CuratorFramework client, String path, Watcher watcher) {
    try {
      return client.getChildren().usingWatcher(watcher).forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("getChildren(" + path + ")", e);
    }
    return null;
  }

  public static void setData(CuratorFramework client, String path, byte[] payload) {
    try {
      client.setData().forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException("setData(" + path + ")", e);
    }
  }

  public static void setDataAsync(CuratorFramework client, String path, byte[] payload) {
    try {
      client.setData().inBackground().forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException("setDataAsync(" + path + ")", e);
    }
  }

  public static List<ACL> getACL(CuratorFramework client, String path) {
    try {
      return client.getACL().forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("getACL(" + path + ")", e);
    }
    return null;
  }

  public static void setACL(CuratorFramework client, String path, List<ACL> acls) {
    try {
      client.setACL().withACL(acls).forPath(path);
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      throw new RuntimeException("setACL(" + path + ")", e);
    }
  }
}
