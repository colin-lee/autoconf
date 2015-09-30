package me.config.impl;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import me.config.api.IChangeableConfig;
import me.config.api.IFileListener;
import me.config.watcher.FileUpdateWatcher;
import org.apache.curator.framework.CuratorFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于远程zookeeper文件的配置,启用本地文件缓存.<br/>
 * 1.启动首先查找本地配置,如果有的话,会先使用本地配置. <br/>
 * 2.同时启动异步线程检查远程zookeeper的配置. <br/>
 * 3.远程zookeeper改动内容会同步些往本地文件缓存. <br/>
 * Created by lirui on 2015/9/30.
 */
public class ZookeeperConfigWithCache extends ZookeeperConfig implements IChangeableConfig {
	private static final ExecutorService executors = cachedExecutors();
	private final File cacheFile;

	public ZookeeperConfigWithCache(String name, String basePath, List<String> paths, CuratorFramework client, File cacheFile) {
		super(name, basePath, paths, client);
		this.cacheFile = cacheFile;
	}

	/**
	 * 异步加载线程不易过多,并且完成加载之后,基本用不到了,所以用cachedPool
	 *
	 * @return ExecutorService
	 */
	static ExecutorService cachedExecutors() {
		return new ThreadPoolExecutor(0, 5,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(10));
	}

	@Override
	public void start() {
		//有本地配置就先从本地加载
		if (cacheFile.exists()) {
			try {
				copyOf(Files.toByteArray(cacheFile));
				//异步检查zookeeper中配置
				executors.submit(new Runnable() {
					@Override
					public void run() {
						ZookeeperConfigWithCache.super.start();
					}
				});
			} catch (IOException e) {
				log.error("cannot read {}", cacheFile);
				super.start();
			}
		} else {
			//本地没有则直接从zookeeper加载
			super.start();
		}
		// 注册本地配置变更通知回调
		FileUpdateWatcher.getInstance().watch(cacheFile.toPath(), new IFileListener() {
			@Override
			public void changed(Path path, byte[] content) {
				log.info("local change: {}", path);
				reload(content);
			}
		});
	}

	@Override
	protected void reload(byte[] content) {
		super.reload(content);
		if (hasChanged(content)) {
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
