package me.config;

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
	private final Path localPath;

	public LocalConfig(String name, Path localPath) {
		super(name);
		this.localPath = localPath;
		try {
			this.copyOf(Files.toByteArray(localPath.toFile()));
		} catch (IOException e) {
			this.copyOf(new byte[0]);
			log.error("configName={}, localPath={}", name, localPath, e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("LocalConfig{");
		sb.append("name='").append(getName()).append('\'');
		sb.append(", localPath=").append(localPath);
		sb.append('}');
		return sb.toString();
	}
}
