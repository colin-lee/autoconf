package me.config;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

/**
 * 基于本地文件的配置
 * Created by lirui on 2015/9/23.
 */
public class LocalConfig extends ConfigCache implements IConfig {
    private static final Logger log = LoggerFactory.getLogger(LocalConfig.class);
    private final String name;
    private final Path localPath;
    private Set<IConfigChangeListener> listeners = Sets.newConcurrentHashSet();

    public LocalConfig(String name, Path localPath) {
        this.name = name;
        this.localPath = localPath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addListener(IConfigChangeListener listener) {
        addListener(listener, true);
    }

    @Override
    public void addListener(IConfigChangeListener listener, boolean loadAfterRegister) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (loadAfterRegister) {
                try {
                    listener.dataChanged(this);
                } catch (Exception e) {
                    log.error("cannot reload " + name, e);
                }
            }
        }
    }

    @Override
    public void removeListener(IConfigChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    void notifyListener() {
        for (IConfigChangeListener i : listeners) {
            log.info("{} changed, notify {}", name, i);
            try {
                i.dataChanged(this);
            } catch (Exception e) {
                log.error("cannot reload " + name, e);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigFile{");
        sb.append("name='").append(name).append('\'');
        sb.append(", localPath=").append(localPath);
        sb.append('}');
        return sb.toString();
    }
}
