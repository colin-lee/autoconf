package com.github.autoconf.impl;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.autoconf.helper.ZookeeperUtil.*;
import static org.apache.zookeeper.Watcher.Event.EventType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * zookeeper配置
 * Created by lirui on 2015-09-29 20:37.
 */
public class RemoteConfigTest {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteConfigTest.class);
  private static TestingServer server;
  private static CuratorFramework client;

  @BeforeClass
  public static void beforeClass() throws Exception {
    server = new TestingServer();
    String servers = server.getConnectString();
    RetryPolicy policy = new BoundedExponentialBackoffRetry(1000, 60000, 10);
    client = CuratorFrameworkFactory.newClient(servers, policy);
    client.start();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Closeables.close(client, true);
    Closeables.close(server, true);
  }

  @Test
  public void testLoad() throws Exception {
    String basePath = "/auto/config/test";
    ArrayList<String> paths = Lists.newArrayList("127.0.0.1:8080", "127.0.0.1", "profile", "appName");
    RemoteConfig config = new RemoteConfig("application.properties", basePath, paths, client);
    config.start();
    assertThat(config.getInt("a"), is(0));
    //验证创建app独有配置
    String appPath = ZKPaths.makePath(basePath, "appName");
    String s = "a=1";
    create(client, appPath, newBytes(s));
    assertThat(newString(getData(client, appPath)), is(s));
    busyWait();
    assertThat(config.getInt("a"), is(1));

    //增加或者删除无用节点不影响当前配置
    String notUsed = ZKPaths.makePath(basePath, "notUsed");
    create(client, notUsed, newBytes("null"));
    busyWait();
    assertThat(config.getInt("a"), is(1));
    delete(client, notUsed);
    busyWait();
    assertThat(config.getInt("a"), is(1));

    //更高优先级的profile配置被创建,切换配置
    s = "a = 2";
    String profilePath = ZKPaths.makePath(basePath, "profile");
    create(client, profilePath, newBytes(s));
    assertThat(newString(getData(client, profilePath)), is(s));
    busyWait();
    assertThat(config.getInt("a"), is(2));

    //更高优先级的ip配置创建,切换配置
    s = "a = 3";
    String ipPath = ZKPaths.makePath(basePath, "127.0.0.1");
    create(client, ipPath, newBytes(s));
    assertThat(newString(getData(client, ipPath)), is(s));
    busyWait();
    assertThat(config.getInt("a"), is(3));

    //修改低优先级配置不影响当前使用的高优先级配置
    s = "b = 4";
    setData(client, profilePath, newBytes(s));
    assertThat(newString(getData(client, profilePath)), is(s));
    busyWait();
    assertThat(config.getInt("a"), is(3));
    assertThat(config.getInt("b"), is(0));

    //删除高优先级配置,自动降级到低优先级配置
    delete(client, ipPath);
    busyWait();
    assertThat(config.getInt("a"), is(0));
    assertThat(config.getInt("b"), is(4));
    delete(client, profilePath);
    busyWait();
    assertThat(config.getInt("a"), is(1));
    assertThat(config.getInt("b"), is(0));

    //删除basePath目录,回退到没有任何配置的状态
    delete(client, basePath);
    busyWait();
    assertThat(config.getInt("a"), is(0));

    //测试全部删除后新增配置,也能生效
    create(client, ipPath, newBytes(s));
    busyWait();
    assertThat(config.getInt("b"), is(4));
  }

  /**
   * 测试zookeeper的watch功能
   *
   * @throws Exception
   */
  @Test
  public void testListener() throws Exception {
    String path = "/test/listener";
    final AtomicInteger at = new AtomicInteger(-1);
    final Watcher watcher = new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        Event.EventType t = event.getType();
        at.set(t.getIntValue());
        String p = event.getPath();
        List<String> s = getChildren(client, p, this);
        getData(client, p, this);
        exists(client, p, this);
        LOG.info("value={}, {}, {}, children:{}", t.getIntValue(), t, p, s);
      }
    };
    exists(client, path, watcher);
    LOG.info("create {}", path);
    ensure(client, path);
    busyWait();
    getChildren(client, path, watcher);
    assertThat(at.get(), is(NodeCreated.getIntValue()));
    LOG.info("setData {}", path);
    setData(client, path, newBytes("a=5"));
    busyWait();
    assertThat(at.get(), is(NodeDataChanged.getIntValue()));
    String child = ZKPaths.makePath(path, "child");
    LOG.info("create {}", child);
    create(client, child, newBytes("b=1"));
    busyWait();
    assertThat(at.get(), is(NodeChildrenChanged.getIntValue()));
    LOG.info("setData {}", child);
    setData(client, child, newBytes("b=2"));
    busyWait();
    LOG.info("delete {}", child);
    delete(client, child);
    busyWait();
    assertThat(at.get(), is(NodeChildrenChanged.getIntValue()));
    LOG.info("delete {}", path);
    delete(client, path);
    busyWait();
    assertThat(at.get(), is(NodeDeleted.getIntValue()));
    create(client, path);
    busyWait();
    assertThat(at.get(), is(NodeCreated.getIntValue()));
  }

  private void busyWait() throws InterruptedException {
    Thread.sleep(100);
  }
}
