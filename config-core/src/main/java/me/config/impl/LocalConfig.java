package me.config.impl;

import com.google.common.io.Files;
import me.config.api.IChangeableConfig;
import me.config.base.ChangeableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 基于本地文件的配置
 * Created by lirui on 2015/9/23.
 */
public class LocalConfig extends ChangeableConfig implements IChangeableConfig {
	private final Logger log = LoggerFactory.getLogger(LocalConfig.class);
	private final Path path;

	public LocalConfig(String name, Path path) {
		super(name);
		this.path = path;
		try {
			this.copyOf(Files.toByteArray(path.toFile()));
		} catch (IOException e) {
			this.copyOf(new byte[0]);
			log.error("configName={}, path={}", name, path, e);
		}
	}

	public Path getPath() {
		return path;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("LocalConfig{");
		sb.append("name='").append(getName()).append('\'');
		sb.append(", path=").append(path);
		sb.append('}');
		return sb.toString();
	}
}
