package me.config;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 消息总线，允许注册消息
 * Created by lirui on 15/9/24.
 */
public class EventBus implements IChangeable {
    private final Logger log = LoggerFactory.getLogger(EventBus.class);
    private final Set<IConfigChangeListener> listeners = Sets.newConcurrentHashSet();
    private final IConfig config;

    public EventBus(IConfig config) {
        this.config = config;
    }

    public void addListener(IConfigChangeListener listener) {
        addListener(listener, true);
    }

    public void addListener(IConfigChangeListener listener, boolean loadAfterRegister) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (loadAfterRegister) {
                try {
                    listener.dataChanged(config);
                } catch (Exception e) {
                    log.error("cannot reload " + config.getName(), e);
                }
            }
        }
    }

    public void removeListener(IConfigChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void notifyListeners() {
        for (IConfigChangeListener i : listeners) {
            log.info("{} changed, notify {}", config.getName(), i);
            try {
                i.dataChanged(config);
            } catch (Exception e) {
                log.error("cannot reload " + config.getName(), e);
            }
        }
    }
}
