package me.config.watcher;

import com.google.common.io.Files;
import me.config.LocalConfig;
import me.config.api.IChangeListener;
import me.config.api.IConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * 测试本地配置内容修改触发回调功能. mac系统上失效,会屏蔽这个用例.
 * Created by lirui on 2015-09-29 15:12.
 */
public class DirectoryWatcherTest {
	private static boolean isMac = false;
	private final Logger log = LoggerFactory.getLogger(DirectoryWatcherTest.class);

	@BeforeClass
	public static void beforeClass() throws Exception {
		String os = System.getProperties().getProperty("os.name");
		isMac = os.toLowerCase().contains("mac");
	}

	@Test
	public void testCallback() throws Exception {
		if (isMac) return;
		DirectoryWatcher watcher = DirectoryWatcher.getInstance();
		File d1 = Files.createTempDir();
		File f1 = File.createTempFile("conf-", ".ini", d1);
		File d2 = Files.createTempDir();
		File f2 = File.createTempFile("conf-", ".txt", d2);
		//mac系统上获取回调通知特别慢,所以通过一个计数器来做忙等待.
		final AtomicInteger num = new AtomicInteger(0);
		IChangeListener listener = new IChangeListener() {
			@Override
			public void changed(IConfig config) {
				log.info("{} updated", config.getName());
				num.incrementAndGet();
			}
		};
		try {
			write("a=1".getBytes(), f1);
			write("b=2".getBytes(), f2);
			LocalConfig c1 = new LocalConfig("c1", f1.toPath());
			LocalConfig c2 = new LocalConfig("c2", f2.toPath());
			c1.addListener(listener, false);
			c2.addListener(listener, false);

			assertThat(c1.get("a"), is("1"));
			assertThat(c2.get("b"), is("2"));
			watcher.watch(c1).watch(c2);
			watcher.start();

			//测试修改文件内容
			write("a=3 ".getBytes(), f1);
			write("b=4 ".getBytes(), f2);

			busyWait(num); //等待检测完成, Mac上特别慢
			assertThat(c1.get("a"), is("3"));
			assertThat(c2.get("b"), is("4"));

			num.set(0);
			//测试删除文件
			delete(f1);
			delete(f2);
			busyWait(num); //等待检测完成, Mac上特别慢
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

	private void busyWait(final AtomicInteger num) throws InterruptedException {
		int tries = 0;
		while (++tries < 1000) {
			Thread.sleep(100);
			if (num.get() > 1) {
				log.info("delay {} ms", 100 * tries);
				return;
			}
		}
		log.error("detect timeout, delay {}ms", 100 * tries);
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
}