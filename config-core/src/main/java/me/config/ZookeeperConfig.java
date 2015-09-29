package me.config;

import me.config.api.IChangeableConfig;
import me.config.base.ChangeableConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 基于远程zookeeper文件的配置
 * Created by lirui on 2015/9/28.
 */
public class ZookeeperConfig extends ChangeableConfig implements IChangeableConfig {
	private final Logger log = LoggerFactory.getLogger(ZookeeperConfig.class);
	private final String basePath;
	private final List<String> paths;
	private final CuratorFramework client;
	public ZookeeperConfig(String name, String basePath, List<String> paths, CuratorFramework client) {
		super(name);
		this.basePath = basePath;
		this.paths = paths;
		this.client = client;
		initFromZookeeper();
	}

	private void initFromZookeeper() {
		for(String i: paths) {
			String path = ZKPaths.makePath(basePath, i);
			client.getData().forPath(path);
		}

		try {
			byte[] bytes = client.getData().forPath(basePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ZookeeperConfig{");
		sb.append("name='").append(getName()).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
