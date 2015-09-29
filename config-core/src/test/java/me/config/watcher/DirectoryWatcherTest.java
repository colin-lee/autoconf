package me.config.watcher;

import com.google.common.io.Files;
import me.config.LocalConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * 测试本地配置内容修改触发回调功能
 * Created by lirui on 2015-09-29 15:12.
 */
public class DirectoryWatcherTest {
	private final Logger log = LoggerFactory.getLogger(DirectoryWatcherTest.class);

	@Test
	public void testCallback() throws Exception {
		DirectoryWatcher watcher = DirectoryWatcher.getInstance();
		File d1 = Files.createTempDir();
		File f1 = File.createTempFile("conf-", ".ini", d1);
		File d2 = Files.createTempDir();
		File f2 = File.createTempFile("conf-", ".txt", d2);
		try {
			write("a=1".getBytes(), f1);
			write("b=2".getBytes(), f2);
			LocalConfig c1 = new LocalConfig("c1", f1.toPath());
			LocalConfig c2 = new LocalConfig("c2", f2.toPath());
			assertThat(c1.get("a"), is("1"));
			assertThat(c2.get("b"), is("2"));
			watcher.watch(c1).watch(c2);
			watcher.start();
			Thread.sleep(1000);

			//测试修改文件内容
			write("a=3 ".getBytes(), f1);
			write("b=4 ".getBytes(), f2);
			Thread.sleep(4000); //等待检测完成
			assertThat(c1.get("a"), is("3"));
			assertThat(c2.get("b"), is("4"));

			//测试删除文件
			delete(f1);
			delete(f2);
			assertThat(c1.get("a"), nullValue());
			assertThat(c2.get("b"), nullValue());
		} finally {
			delete(f1);
			delete(d1);
			delete(f2);
			delete(d2);
			watcher.shutdown();
		}
	}

	private void write(byte[] bytes, File f) throws IOException {
		log.info("write {} bytes into {}", bytes.length, f);
		Files.write(bytes, f);
	}

	private void delete(File f) {
		if (!f.exists()) return;
		log.info("delete {}", f);
		if (!f.delete()) {
			f.deleteOnExit();
		}
	}

	@Test
	public void testWatchOne() throws Exception {
		Thread thread = null;
		try {
			final WatchService watcher = FileSystems.getDefault().newWatchService();
			File d1 = new File("/tmp");//Files.createTempDir();
			File f1 = new File("/tmp/colin.ini");//File.createTempFile("one-", ".ini", d1);
			Path path = d1.toPath();
			WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			log.info("monitor {}, key={}", path, key);
			key.reset();
			Task task = new Task(watcher);
			thread = new Thread(task, "DirWatcher");
			thread.start();

			write("test".getBytes(), f1);
			File.createTempFile("colin-", ".txt", d1);
			Thread.sleep(2000000);

			task.shutdown();
		} finally {
			if (thread != null) {
				thread.interrupt();
			}
		}
	}

	private class Task implements Runnable {
		private final WatchService watchService;
		private boolean running;

		private Task(WatchService watchService) {
			this.watchService = watchService;
		}

		public void shutdown() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			while (running) {
				try {
					WatchKey key = watchService.take();
					if (key != null) {
						for (WatchEvent<?> event : key.pollEvents()) {
							WatchEvent.Kind kind = event.kind();
							if (kind == StandardWatchEventKinds.OVERFLOW) {
								continue;
							}
							WatchEvent<Path> ev = (WatchEvent<Path>) event;
							Path context = ev.context();
							log.info("{}, {}", kind, context.toFile());
						}
						key.reset();
					}
				} catch (InterruptedException x) {
					log.error("{} was interrupted, now EXIT", Thread.currentThread().getName());
					return;
				} catch (Exception e) {
					log.error("error", e);
				}
			}
			running = false;
			try {
				watchService.close();
			} catch (IOException ignored) {
			}
		}
	}
}