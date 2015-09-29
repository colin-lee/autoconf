package me.config;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * zookeeper配置
 * Created by lirui on 2015-09-29 20:37.
 */
public class ZookeeperConfigTest {
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
		ZookeeperConfig config = new ZookeeperConfig("test", basePath, paths, client);
		assertThat(config.getInt("a"), is(0));
	}
}