package me.config;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 基于本地文件的配置
 * Created by lirui on 2015/9/23.
 */
public class LocalConfig extends ConfigCache implements IConfig {
    private final Logger log = LoggerFactory.getLogger(LocalConfig.class);
    private final String name;
    private final Path localPath;
    private final IChangeable eventBus;

    public LocalConfig(String name, Path localPath) {
        this.name = name;
        this.localPath = localPath;
        this.eventBus = new EventBus(this);
        try {
            this.setContent(Files.toByteArray(localPath.toFile()));
        } catch (IOException e) {
            this.setContent(new byte[0]);
            log.error("configName={}, localPath={}", name, localPath, e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void addListener(IConfigChangeListener listener) {
        eventBus.addListener(listener);
    }

    public void addListener(IConfigChangeListener listener, boolean loadAfterRegister) {
        eventBus.addListener(listener, loadAfterRegister);
    }

    public void removeListener(IConfigChangeListener listener) {
        eventBus.removeListener(listener);
    }

    public void notifyListeners() {
        eventBus.notifyListeners();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LocalConfig{");
        sb.append("name='").append(name).append('\'');
        sb.append(", localPath=").append(localPath);
        sb.append('}');
        return sb.toString();
    }
}
