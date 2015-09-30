package me.config.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import me.config.api.IChangeableConfig;
import me.config.api.IFileListener;
import me.config.watcher.FileUpdateWatcher;
import org.apache.curator.framework.CuratorFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 基于远程zookeeper文件的配置,启用本地文件缓存.<br/>
 * 1.启动首先查找本地配置,如果有的话,会先使用本地配置. <br/>
 * 2.同时启动异步线程检查远程zookeeper的配置. <br/>
 * 3.远程zookeeper改动内容会同步些往本地文件缓存. <br/>
 * Created by lirui on 2015/9/30.
 */
public class ZookeeperConfigWithCache extends ZookeeperConfig implements IChangeableConfig {
	private static final Set<ZookeeperConfig> items = Sets.newConcurrentHashSet();
	private final File cacheFile;

	public ZookeeperConfigWithCache(String name, String basePath, List<String> paths, CuratorFramework client, File cacheFile) {
		super(name, basePath, paths, client);
		this.cacheFile = cacheFile;
	}

	@Override
	public void start() {
		//有本地配置就先从本地加载
		if (cacheFile.exists()) {
			try {
				copyOf(Files.toByteArray(cacheFile));
				//异步检查zookeeper中配置
				items.add(this);
			} catch (IOException e) {
				log.error("cannot read {}", cacheFile);
				initZookeeper();
			}
		} else {
			//本地没有则直接从zookeeper加载
			initZookeeper();
		}
		//注册本地配置变更通知回调
		FileUpdateWatcher.getInstance().watch(cacheFile.toPath(), new IFileListener() {
			@Override
			public void changed(Path path, byte[] content) {
				log.info("local change: {}", path);
				reload(content);
			}
		});
		//延迟加载zookeeper上的配置,避免服务启动过慢
		Thread zkThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ignored) {
				}
				for (ZookeeperConfig i : items) {
					i.initZookeeper();
				}
			}
		}, "asyncLoadFromZookeeper");
		zkThread.setDaemon(true);
		zkThread.start();
	}

	@Override
	protected void reload(byte[] content) {
		if (hasChanged(content)) {
			copyOf(content);
			notifyListeners();
			try {
				Files.write(content, cacheFile);
			} catch (IOException e) {
				log.error("cannot write {}", cacheFile);
			}
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("cacheFile", cacheFile)
				.add("zkPath", getZkPath())
				.toString();
	}
}
