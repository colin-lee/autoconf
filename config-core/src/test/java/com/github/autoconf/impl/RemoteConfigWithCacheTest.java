package com.github.autoconf.impl;

import com.github.autoconf.TestHelper;
import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.helper.ZookeeperUtil;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 验证本地cache等功能
 * Created by lirui on 2015-09-30 18:56.
 */
public class RemoteConfigWithCacheTest {
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
  public void testFirstLoadFile() throws Exception {
    String basePath = "/auto/config/testCache";
    ArrayList<String> paths = Lists.newArrayList("profile", "appName");
    File cacheFile = File.createTempFile("cache-", ".ini");
    TestHelper.writeFile(ZookeeperUtil.newBytes("a=1"), cacheFile);
    RemoteConfigWithCache config = new RemoteConfigWithCache("cache.ini", basePath, paths, client, cacheFile);
    config.setDelaySeconds(1);
    config.start();
    assertThat(config.getInt("a"), is(1));

    //等待从服务端对比加载
    Thread.sleep(2000);
    //这个时候远程服务上还没有,所以不会引起配置变更
    assertThat(config.getInt("a"), is(1));
    String appPath = ZKPaths.makePath(basePath, "appName");

    //使用忙等待
    final AtomicInteger num = new AtomicInteger(0);
    config.addListener(new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    }, false);
    ZookeeperUtil.create(client, appPath, ZookeeperUtil.newBytes("a=2"));
    TestHelper.busyWait(num);
    assertThat(config.getInt("a"), is(2));
    Thread.sleep(100);
    assertThat(ZookeeperUtil.newString(Files.toByteArray(cacheFile)), is("a=2"));

    //服务变更,收到通知
    ZookeeperUtil.setData(client, appPath, ZookeeperUtil.newBytes("a=3"));
    TestHelper.busyWait(num);
    assertThat(config.getInt("a"), is(3));
    assertThat(ZookeeperUtil.newString(Files.toByteArray(cacheFile)), is("a=3"));

    //删除zookeeper配置,本地配置也要对应变更
    num.set(0);
    ZookeeperUtil.delete(client, appPath);
    TestHelper.busyWait(num);
    assertThat(config.getInt("a"), is(0));
    Thread.sleep(100);
    assertThat(ZookeeperUtil.newString(Files.toByteArray(cacheFile)), is(""));

    TestHelper.deleteFile(cacheFile);
  }

  /**
   * 本地没有配置,必须从远程服务拉取
   *
   * @throws Exception
   */
  @Test
  public void testLoadFromRemote() throws Exception {
    String basePath = "/auto/config/loadRemote";
    ArrayList<String> paths = Lists.newArrayList("profile", "appName");
    String appPath = ZKPaths.makePath(basePath, "appName");
    String s = "a = 1";
    ZookeeperUtil.create(client, appPath, ZookeeperUtil.newBytes(s));
    File tempDir = Files.createTempDir();
    File cacheFile = new File(tempDir.getPath() + "/cache.ini");
    RemoteConfigWithCache config = new RemoteConfigWithCache("cache.ini", basePath, paths, client, cacheFile);
    config.setDelaySeconds(1);
    config.start();
    assertThat(config.getInt("a"), is(1));
    assertThat(ZookeeperUtil.newString(Files.toByteArray(cacheFile)), is(s));
    TestHelper.deleteFile(cacheFile);
    TestHelper.deleteFile(tempDir);
  }
}
