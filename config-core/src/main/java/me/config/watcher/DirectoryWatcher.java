package me.config.watcher;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sun.nio.file.SensitivityWatchEventModifier;
import me.config.LocalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 监测本地目录的文件变更
 * Created by lirui on 2015-09-29 14:38.
 */
public class DirectoryWatcher implements Runnable {
	private final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);
	private final Multimap<Path, LocalConfig> watches = LinkedHashMultimap.create();
	private final Map<WatchKey, Path> keys = Maps.newConcurrentMap();
	private WatchService watchService;
	private boolean running;

	private DirectoryWatcher() {
		try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			log.error("cannot build watchService", e);
		}
	}

	public static DirectoryWatcher getInstance() {
		return LazyHolder.instance;
	}

	public void shutdown() {
		running = false;
	}

	public DirectoryWatcher watch(LocalConfig config) {
		Path parent = config.getPath().getParent();
		if (!watches.containsKey(parent)) {
			try {
				WatchEvent.Kind[] events = {ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE};
				WatchKey key = parent.register(watchService, events, SensitivityWatchEventModifier.HIGH);
				keys.put(key, parent);
				log.info("monitor directory {}", parent);
			} catch (IOException e) {
				log.error("cannot register path:{}", parent, e);
			}
		}
		watches.put(parent, config);
		return this;
	}

	public void start() {
		Thread t = new Thread(this, "LocalFileUpdateWatcher");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			WatchKey key = null;
			try {
				key = watchService.take();
				if (key != null) {
					Path base = keys.get(key);
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind kind = event.kind();
						if (kind == OVERFLOW) {
							continue;
						}
						WatchEvent<Path> ev = cast(event);
						Path context = ev.context();
						Path child = base.resolve(context);
						log.info("{}, {}", kind, child);

						List<LocalConfig> fresh = Lists.newArrayList();
						for (LocalConfig c : watches.get(base)) {
							if (c.getPath().compareTo(child) == 0) {
								fresh.add(c);
							}
						}
						if (fresh.size() > 0) {
							byte[] content = new byte[0];
							if (kind != ENTRY_DELETE) {
								content = Files.readAllBytes(child);
							}
							for (LocalConfig c : fresh) {
								if (!Arrays.equals(content, c.getContent())) {
									c.copyOf(content);
									c.notifyListeners();
									log.info("update config:{}", c);
								}
							}
						}
					}
				}
			} catch (InterruptedException x) {
				log.error("{} was interrupted, now EXIT", Thread.currentThread().getName());
				break;
			} catch (Exception e) {
				log.error("watches: {}", watches.keySet(), e);
			} finally {
				if (key != null) {
					key.reset();
				}
			}
		}
		running = false;
		try {
			watchService.close();
		} catch (IOException ignored) {
		}
	}

	@SuppressWarnings("unchecked")
	private <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private static final class LazyHolder {
		private static final DirectoryWatcher instance = new DirectoryWatcher();
	}
}
