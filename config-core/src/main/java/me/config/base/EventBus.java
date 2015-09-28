package me.config.base;

import com.google.common.collect.Sets;
import me.config.api.IChangeListener;
import me.config.api.IChangeable;
import me.config.api.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 消息总线，允许注册消息
 * Created by lirui on 15/9/24.
 */
public class EventBus implements IChangeable {
	private final Logger log = LoggerFactory.getLogger(EventBus.class);
	private final Set<IChangeListener> listeners = Sets.newConcurrentHashSet();
	private final IConfig config;

	public EventBus(IConfig config) {
		this.config = config;
	}

	public void addListener(IChangeListener listener) {
		addListener(listener, true);
	}

	public void addListener(IChangeListener listener, boolean loadAfterRegister) {
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

	public void removeListener(IChangeListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	public void notifyListeners() {
		for (IChangeListener i : listeners) {
			log.info("{} changed, notify {}", config.getName(), i);
			try {
				i.dataChanged(config);
			} catch (Exception e) {
				log.error("cannot reload " + config.getName(), e);
			}
		}
	}
}
