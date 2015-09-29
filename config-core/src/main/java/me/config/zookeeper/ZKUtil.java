package me.config.zookeeper;

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
public class ZKUtil {
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final Charset GBK = Charset.forName("GBK");

	private ZKUtil() {
	}

	public static String newString(byte[] data) {
		return newString(data, UTF8);
	}

	public static String newString(byte[] data, Charset charset) {
		if (data == null) return null;
		return new String(data, charset);
	}

	public static byte[] newBytes(String s) {
		if (s == null) return null;
		return s.getBytes(UTF8);
	}

	public static byte[] newBytes(String s, Charset charset) {
		if (s == null) return null;
		return s.getBytes(charset);
	}

	public static String ensure(CuratorFramework client, String path) {
		try {
			client.checkExists().creatingParentContainersIfNeeded().forPath(path);
		} catch (Exception e) {
			throw new RuntimeException("ensure(" + path + "), reason: " + e.getMessage());
		}
		return path;
	}

	public static Stat exists(CuratorFramework client, String path) {
		try {
			return client.checkExists().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("exists(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static Stat exists(CuratorFramework client, String path, Watcher watcher) {
		try {
			return client.checkExists().usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("exists(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static void create(CuratorFramework client, String path) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path);
		} catch (Exception e) {
			throw new RuntimeException("create(" + path + "), reason: " + e.getMessage());
		}
	}

	public static void create(CuratorFramework client, String path, byte[] payload) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path, payload);
		} catch (Exception e) {
			throw new RuntimeException("create(" + path + "), reason: " + e.getMessage());
		}
	}

	public static void create(CuratorFramework client, String path, byte[] payload, CreateMode mode) {
		try {
			client.create().creatingParentsIfNeeded().withMode(mode).forPath(path, payload);
		} catch (Exception e) {
			throw new RuntimeException("create(" + path + "), reason: " + e.getMessage());
		}
	}

	public static void create(CuratorFramework client, String path, byte[] payload,
							  CreateMode mode, List<ACL> aclList) {
		try {
			client.create().creatingParentsIfNeeded().withMode(mode).withACL(aclList).forPath(path, payload);
		} catch (Exception e) {
			throw new RuntimeException("create(" + path + "), reason: " + e.getMessage());
		}
	}

	/**
	 * 删除一个目录，如果必要会删除其子目录
	 */
	public static void delete(CuratorFramework client, String path) {
		try {
			client.delete().deletingChildrenIfNeeded().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("delete(" + path + "), reason: " + e.getMessage());
		}
	}

	public static void guaranteedDelete(CuratorFramework client, String path) {
		try {
			client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("guaranteedDelete(" + path + "), reason: " + e.getMessage());
		}
	}

	public static byte[] getData(CuratorFramework client, String path) {
		try {
			return client.getData().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("getData(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static byte[] getData(CuratorFramework client, String path, Watcher watcher) {
		try {
			return client.getData().usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("getData(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static List<String> getChildren(CuratorFramework client, String path) {
		try {
			return client.getChildren().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("getChildren(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static List<String> watchedGetChildren(CuratorFramework client, String path) {
		try {
			return client.getChildren().watched().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("watchedGetChildren(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static List<String> watchedGetChildren(CuratorFramework client, String path, Watcher watcher) {
		try {
			return client.getChildren().usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("watchedGetChildren(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static void setData(CuratorFramework client, String path, byte[] payload) {
		try {
			client.setData().forPath(path, payload);
		} catch (Exception e) {
			throw new RuntimeException("setData(" + path + "), reason: " + e.getMessage());
		}
	}

	public static void setDataAsync(CuratorFramework client, String path, byte[] payload) {
		try {
			client.setData().inBackground().forPath(path, payload);
		} catch (Exception e) {
			throw new RuntimeException("setDataAsync(" + path + "), reason: " + e.getMessage());
		}
	}

	public static List<ACL> getACL(CuratorFramework client, String path) {
		try {
			return client.getACL().forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("getACL(" + path + "), reason: " + e.getMessage());
		}
		return null;
	}

	public static void setACL(CuratorFramework client, String path, List<ACL> acls) {
		try {
			client.setACL().withACL(acls).forPath(path);
		} catch (KeeperException.NoNodeException ignored) {
		} catch (Exception e) {
			throw new RuntimeException("setACL(" + path + "), reason: " + e.getMessage());
		}
	}
}