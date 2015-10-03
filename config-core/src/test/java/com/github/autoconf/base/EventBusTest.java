package com.github.autoconf.base;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeable;
import com.github.autoconf.api.IConfig;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试EventBus
 * Created by lirui on 2015-09-28 17:05.
 */
public class EventBusTest {
  @Test
  public void testBus() throws Exception {
    IConfig conf = new ChangeableConfig("test");
    IChangeable bus = new EventBus(conf);
    final AtomicInteger count = new AtomicInteger(0);
    IChangeListener listener = new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        count.incrementAndGet();
      }
    };
    bus.addListener(listener);
    //衡量注册后被自动调用1次
    assertThat(count.get(), is(1));
    bus.notifyListeners();
    //回调函数被调用
    assertThat(count.get(), is(2));
    //注销后不会再获得通知
    bus.removeListener(listener);
    bus.notifyListeners();
    assertThat(count.get(), is(2));
  }

  @Test
  public void testReloadException() throws Exception {
    IConfig conf = new ChangeableConfig("test");
    IChangeable bus = new EventBus(conf);
    final AtomicInteger count = new AtomicInteger(0);
    IChangeListener listener = new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        count.incrementAndGet();
      }
    };
    //注明不做首次调用
    bus.addListener(listener, false);
    assertThat(count.get(), is(0));
    bus.addListener(new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        throw new RuntimeException("some error happened");
      }
    });
    bus.notifyListeners();
    //回调函数被调用，不受其他listener抛出异常影响
    assertThat(count.get(), is(1));
  }
}
