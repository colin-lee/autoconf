package me.config.impl;

import com.google.common.base.MoreObjects;
import me.config.api.IChangeableConfig;
import me.config.base.ChangeableConfig;
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

import static me.config.zookeeper.ZookeeperUtil.*;

/**
 * 基于远程zookeeper文件的配置
 * Created by lirui on 2015/9/28.
 */
public class ZookeeperConfig extends ChangeableConfig implements IChangeableConfig {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final String zkPath;
	private final List<String> paths;
	private final CuratorFramework client;
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
					log.warn("skip {}, {}", t, p);
			}
		}
	};
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
					log.warn("skip {}, {}", t, p);
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

	public ZookeeperConfig(String name, String zkPath, List<String> paths, CuratorFramework client) {
		super(name);
		this.zkPath = zkPath;
		this.paths = paths;
		this.client = client;
	}

	protected void initZookeeper() {
		client.getConnectionStateListenable().addListener(stateListener);
		if (exists(client, zkPath, baseWatcher) != null) {
			loadFromZookeeper();
		}
	}

	public void start() {
		initZookeeper();
	}

	protected void loadFromZookeeper() {
		log.info("{}, zkPath:{}, order:{}", getName(), zkPath, paths);
		List<String> children = getChildren(client, zkPath, baseWatcher);
		boolean found = false;
		//按照特定顺序逐个查找配置
		if (children != null && children.size() > 0) {
			log.info("zkPath:{}, children:{}", zkPath, children);
			for (String i : paths) {
				if (!children.contains(i)) continue;
				String path = ZKPaths.makePath(zkPath, i);
				try {
					byte[] content = getData(client, path, leafWatcher);
					if (content != null && content.length > 0) {
						log.info("{}, zkPath:{}", getName(), path);
						log.debug("content:\n{}\n", newString(content));
						reload(content);
						found = true;
						break;
					}
				} catch (Exception e) {
					log.error("cannot load {} from zookeeper, zkPath{}", getName(), zkPath, e);
				}
			}
		}
		if (!found) {
			exists(client, zkPath, baseWatcher);
			log.warn("cannot find {} in zookeeper, zkPath{}", getName(), zkPath);
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
		if (now == null) return true;
		byte[] old = getContent();
		return !Arrays.equals(now, old);
	}

	public String getZkPath() {
		return zkPath;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("zkPath", zkPath)
				.toString();
	}
}
