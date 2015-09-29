package me.config.watcher;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import me.config.LocalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * 监测本地目录的文件变更
 * Created by lirui on 2015-09-29 14:38.
 */
public class DirectoryWatcher implements Runnable {
	private final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);
	private final Multimap<Path, LocalConfig> watches = LinkedHashMultimap.create();
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
				parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
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
			try {
				WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
				log.info("key={}", key);
				if (key != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind kind = event.kind();
						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}
						WatchEvent<Path> ev = cast(event);
						Path context = ev.context();
						log.error("{}, {}", kind, context);
						/*
						Path child = target.resolve(context);
						log.info("[FileUpdateWatcher]\tdetect changed: {}", child);
						*/
					}
					key.reset();
				}
			} catch (InterruptedException x) {
				log.error("{} was interrupted, now EXIT", Thread.currentThread().getName());
				return;
			} catch (Exception e) {
				log.error("watches: {}", watches.keySet(), e);
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
