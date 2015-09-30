package me.config;

import me.config.api.IChangeableConfig;
import me.config.impl.ZookeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 基于远程zookeeper文件的配置
 * Created by lirui on 2015/9/28.
 */
public class ZookeeperConfigWithCache extends ZookeeperConfig implements IChangeableConfig {
	private final Logger log = LoggerFactory.getLogger(ZookeeperConfigWithCache.class);

	public ZookeeperConfigWithCache(String name, String basePath, List<String> paths, CuratorFramework client) {
		super(name, basePath, paths, client);
	}

	@Override
	public String toString() {
		return "ZookeeperConfigWithCache{name=" + getName() + '}';
	}
}
