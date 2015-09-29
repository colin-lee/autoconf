package me.config;

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

import static me.config.zookeeper.ZookeeperUtil.*;
import static org.apache.zookeeper.Watcher.Event.EventType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * zookeeper配置
 * Created by lirui on 2015-09-29 20:37.
 */
public class ZookeeperConfigTest {
	private static TestingServer server;
	private static CuratorFramework client;
	private final Logger log = LoggerFactory.getLogger(ZookeeperConfigTest.class);

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
		//验证创建app独有配置
		String appPath = ZKPaths.makePath(basePath, "appName");
		String s = "a=6";
		create(client, appPath, newBytes(s));
		assertThat(newString(getData(client, appPath)), is(s));
		busyWait();
		assertThat(config.getInt("a"), is(6));
	}

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
				log.info("value={}, {}, {}, children:{}", t.getIntValue(), t, p, s);
			}
		};
		exists(client, path, watcher);
		log.info("create {}", path);
		ensure(client, path);
		busyWait();
		getChildren(client, path, watcher);
		assertThat(at.get(), is(NodeCreated.getIntValue()));
		log.info("setData {}", path);
		setData(client, path, newBytes("a=5"));
		busyWait();
		assertThat(at.get(), is(NodeDataChanged.getIntValue()));
		String child = ZKPaths.makePath(path, "child");
		log.info("create {}", child);
		create(client, child, newBytes("b=1"));
		busyWait();
		assertThat(at.get(), is(NodeChildrenChanged.getIntValue()));
		log.info("setData {}", child);
		setData(client, child, newBytes("b=2"));
		busyWait();
		log.info("delete {}", child);
		delete(client, child);
		busyWait();
		assertThat(at.get(), is(NodeChildrenChanged.getIntValue()));
		log.info("delete {}", path);
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