package me.config;

import com.google.common.base.Splitter;
import me.config.api.IChangeableConfig;
import me.config.api.IConfigFactory;
import me.config.api.IFileListener;
import me.config.base.AbstractConfigFactory;
import me.config.impl.LocalConfig;
import me.config.watcher.FileUpdateWatcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;

/**
 * 本地配置工厂
 * Created by lirui on 2015-09-30 22:25.
 */
public class LocalConfigFactory extends AbstractConfigFactory {
	private final Path basePath;

	public LocalConfigFactory(Path basePath) {
		this.basePath = basePath;
	}

	public static IConfigFactory getInstance() {
		return LazyHolder.instance;
	}

	/**
	 * 创建LocalConfig并增加更新回调功能
	 *
	 * @param name 配置名
	 * @return 配置
	 */
	@Override
	protected IChangeableConfig doCreate(String name) {
		Path path = basePath.resolve(name);
		final LocalConfig c = new LocalConfig(name, path);
		FileUpdateWatcher.getInstance().watch(path, new IFileListener() {
			@Override
			public void changed(Path path, byte[] content) {
				log.info("{} changed", path);
				c.copyOf(content);
				c.notifyListeners();
			}
		});
		return c;
	}

	private static class LazyHolder {
		private static final IConfigFactory instance = create();

		/**
		 * 1.扫描配置参数 localConfigPath <br/>
		 * 2.扫描类路径下的 autoconf 目录 <br/>
		 * 3.如果找不到就用java.io.tmpdir <br/>
		 *
		 * @return factory
		 */
		private static IConfigFactory create() {
			Path basePath = scanProperty();
			if (basePath != null) {
				return new LocalConfigFactory(basePath);
			}
			//查找若干文件以便找到classes根目录
			for (String i : Splitter.on(',').split("autoconf,log4j.properties,logback.xml,application.properties")) {
				String s = scanRootPath(i);
				if (s != null) {
					basePath = new File(s).toPath().getParent().resolve("autoconf");
					File root = basePath.toFile();
					if (root.exists() || root.mkdir()) {
						return new LocalConfigFactory(basePath);
					}
				}
			}
			return new LocalConfigFactory(new File(System.getProperty("java.io.tmpdir")).toPath());
		}

		/**
		 * 看是否通过环境变量指明了本地文件cache的路径
		 */
		private static Path scanProperty() {
			String localCachePath = System.getProperty("localConfigPath");
			if (localCachePath != null && localCachePath.length() > 0) {
				File f = new File(localCachePath);
				if (!f.exists()) {
					if (!f.mkdirs()) { // 创建目录失败
						return null;
					}
				}
				return f.toPath();
			}
			return null;
		}

		/**
		 * 扫描配置文件根目录
		 *
		 * @param resource 资源名
		 * @return 找到返回路径否则返回null
		 */
		private static String scanRootPath(String resource) {
			try {
				Enumeration<URL> ps = Thread.currentThread().getContextClassLoader().getResources(resource);
				while (ps.hasMoreElements()) {
					URL url = ps.nextElement();
					String s = url.toString();
					if (s.startsWith("file:/")) {
						String os = System.getProperty("os.name");
						if (os != null && os.toLowerCase().contains("windows")) {
							return s.substring(6);
						} else {
							return s.substring(5);
						}
					}
				}
			} catch (IOException ignored) {
			}
			return null;
		}
	}
}
