package me.config;

/**
 *
 * Created by lirui on 15/9/24.
 */
public abstract class BaseConfig extends ConfigCache implements IConfig {
	private final IChangeable eventBus;

	public BaseConfig() {
		this.eventBus = new EventBus(this);
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
}
