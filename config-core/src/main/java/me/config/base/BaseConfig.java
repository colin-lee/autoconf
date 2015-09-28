package me.config.base;

import me.config.api.IChangeListener;
import me.config.api.IChangeable;
import me.config.api.IChangeableConfig;

/**
 * Created by lirui on 15/9/24.
 */
public abstract class BaseConfig extends Config implements IChangeableConfig {
	private final IChangeable eventBus;

	public BaseConfig() {
		this.eventBus = new EventBus(this);
	}

	public void addListener(IChangeListener listener) {
		eventBus.addListener(listener);
	}

	public void addListener(IChangeListener listener, boolean loadAfterRegister) {
		eventBus.addListener(listener, loadAfterRegister);
	}

	public void removeListener(IChangeListener listener) {
		eventBus.removeListener(listener);
	}

	public void notifyListeners() {
		eventBus.notifyListeners();
	}
}
