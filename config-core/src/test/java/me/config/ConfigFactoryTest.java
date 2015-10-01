package me.config;

import com.google.common.io.Files;
import me.config.api.IChangeListener;
import me.config.api.IChangeableConfig;
import me.config.api.IConfig;
import me.config.watcher.FileUpdateWatcher;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static me.config.zookeeper.ZookeeperUtil.newBytes;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-09-30 22:49.
 */
public class ConfigFactoryTest {
	private Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void testFactory() throws Exception {
		File dir = Files.createTempDir();
		File f1 = dir.toPath().resolve("f1").toFile();
		File f2 = dir.toPath().resolve("f2").toFile();
		File f3 = dir.toPath().resolve("f3").toFile();
		try {
			FileUpdateWatcher.getInstance().start();
			write(newBytes("a=1"), f1);
			write(newBytes("a=2\nb=2"), f2);
			ConfigFactory factory = new ConfigFactory(dir.toPath());
			IChangeableConfig c1 = factory.getConfig("f1");
			IChangeableConfig merge = factory.getConfig("f1,f2,f3");
			assertThat(c1.getInt("a"), is(1));
			assertThat(merge.getInt("a"), is(1));
			assertThat(merge.getInt("b"), is(2));
			//测试本地配置修改更新
			final AtomicInteger num = new AtomicInteger(0);
			merge.addListener(new IChangeListener() {
				@Override
				public void changed(IConfig config) {
					log.info("{} changed", config.getName());
					num.incrementAndGet();
				}
			}, false);
			write(newBytes("a=3"), f1);
			busyWait(num);
			assertThat(merge.getInt("a"), is(3));

			num.set(0);
			write(newBytes("c=4"), f3);
			busyWait(num);
			assertThat(merge.getInt("c"), is(4));
		} finally {
			delete(f1);
			delete(f2);
			delete(f3);
			delete(dir);
		}
	}

	private void busyWait(final AtomicInteger num) throws InterruptedException {
		int tries = 0;
		while (++tries < 1000) {
			Thread.sleep(100);
			if (num.get() > 0) {
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