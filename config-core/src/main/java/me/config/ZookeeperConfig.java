package me.config;

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
	private final Logger log = LoggerFactory.getLogger(ZookeeperConfig.class);
	private final String basePath;
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
					log.warn("skip {}, {}", t, p);
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
					log.warn("skip {}, {}", t, p);
			}
		}
	};
	private final ConnectionStateListener stateListener = new ConnectionStateListener() {
		public void stateChanged(CuratorFramework client, ConnectionState newState) {
			if (newState.equals(ConnectionState.RECONNECTED)) {
				init();
			}
		}
	};

	public ZookeeperConfig(String name, String basePath, List<String> paths, CuratorFramework client) {
		super(name);
		this.basePath = basePath;
		this.paths = paths;
		this.client = client;
		try {
			init();
		} catch (Exception e) {
			log.error("cannot init {}, zkPath{}", getName(), basePath, e);
		}
	}

	private void init() {
		client.getConnectionStateListenable().addListener(stateListener);
		if (exists(client, basePath, baseWatcher) != null) {
			loadFromZookeeper();
		}
	}

	public void loadFromZookeeper() {
		log.info("{}, zkPath:{}, order:{}", getName(), basePath, paths);
		List<String> children = getChildren(client, basePath, baseWatcher);
		boolean found = false;
		//按照特定顺序逐个查找配置
		if (children != null && children.size() > 0) {
			log.info("zkPath:{}, children:{}", basePath, children);
			for (String i : paths) {
				if (!children.contains(i)) continue;
				String path = ZKPaths.makePath(basePath, i);
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
					log.error("cannot load {} from zookeeper, zkPath{}", getName(), basePath, e);
				}
			}
		}
		if (!found) {
			exists(client, basePath, baseWatcher);
			log.warn("cannot find {} in zookeeper, zkPath{}", getName(), basePath);
			copyOf(new byte[0]);
			notifyListeners();
		}
	}

	private void reload(byte[] content) {
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
	private boolean hasChanged(byte[] now) {
		if (now == null) return true;
		byte[] old = getContent();
		return !Arrays.equals(now, old);
	}

	@Override
	public String toString() {
		return "ZookeeperConfig{name=" + getName() + '}';
	}
}
