package com.github.autoconf.helper;

import com.github.autoconf.base.Config;
import com.github.autoconf.base.ProcessInfo;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

import static com.github.autoconf.helper.ZookeeperUtil.newBytes;

/**
 * 基础工具类
 * Created by lirui on 2015-10-01 19:38.
 */
public class ConfigHelper {
  private ConfigHelper() {
  }

  public static Path getConfigPath() {
    return LazyHolder2.CONFIG_PATH;
  }

  public static Config getApplicationConfig() {
    return LazyHolder3.CONFIG;
  }

  public static String getServerInnerIP() {
    return LazyHolder1.INNER_IP;
  }

  public static ProcessInfo getProcessInfo() {
    return LazyHolder4.PROCESS_INFO;
  }

  public static CuratorFramework getDefaultClient() {
    return LazyHolder5.DEFAULT_CLIENT;
  }

  /**
   * <pre>
   * 1.扫描配置参数 CONFIG_PATH
   * 2.扫描类路径下的 autoconf 目录
   * 3.如果找不到就用java.io.tmpdir
   * </pre>
   * @return factory
   */
  private static Path scanConfigPath() {
    Path basePath = scanProperty();
    if (basePath != null) {
      return basePath;
    }
    //查找若干文件以便找到classes根目录
    String files = "autoconf,log4j.properties,logback.xml,application.properties";
    for (String i : Splitter.on(',').split(files)) {
      String s = scanResource(i);
      if (s != null) {
        basePath = new File(s).toPath().getParent().resolve("autoconf");
        File root = basePath.toFile();
        if (root.exists() || root.mkdir()) {
          return basePath;
        }
      }
    }
    return new File(System.getProperty("java.io.tmpdir")).toPath();
  }

  /**
   * 看是否通过环境变量指明了本地文件cache的路径
   */
  private static Path scanProperty() {
    String localCachePath = System.getProperty("CONFIG_PATH");
    if (localCachePath != null && localCachePath.length() > 0) {
      File f = new File(localCachePath);
      if (!f.exists()) {
        if (!f.mkdirs()) { // 创建目录失败
          return null;
        }
      }
      return f.toPath();
    }
    return null;
  }

  /**
   * 在类路径下查找资源
   *
   * @param resource 资源名
   * @return 找到返回路径否则返回null
   */
  private static String scanResource(String resource) {
    try {
      Enumeration<URL> ps = Thread.currentThread().getContextClassLoader().getResources(resource);
      while (ps.hasMoreElements()) {
        URL url = ps.nextElement();
        String s = url.toString();
        if (s.startsWith("file:/")) {
          String os = System.getProperty("os.name");
          if (os != null && os.toLowerCase().contains("windows")) {
            return s.substring(6);
          } else {
            return s.substring(5);
          }
        }
      }
    } catch (IOException ignored) {
    }
    return null;
  }

  /**
   * 扫描配置根目录或者类路径下的application.properties文件并解析
   *
   * @return 加载的配置信息
   */
  private static Config applicationConfig() {
    String name = "application.properties";
    File f = getConfigPath().resolve(name).toFile();
    if (!f.exists()) {
      String s = scanResource(name);
      if (!Strings.isNullOrEmpty(s)) {
        f = new File(s);
      }
    }
    Config c = new Config();
    try {
      c.copyOf(Files.readAllBytes(f.toPath()));
    } catch (IOException ignored) {
    }
    return c;
  }

  private static ProcessInfo scanProcessInfo() {
    Config c = getApplicationConfig();
    ProcessInfo info = new ProcessInfo();
    info.setPath(c.get("zookeeper.basePath", "/cms/config"));
    info.setName(c.get("process.name"));
    info.setIp(c.get("process.ip", getServerInnerIP()));
    info.setProfile(c.get("process.profile", "test"));
    String s = c.get("process.port");
    if (Strings.isNullOrEmpty(s)) {
      try {
        Integer port = WebServer.getHttpPort();
        if (port != null) {
          info.setPort(port.toString());
        }
      } catch (Exception ignored) {
      }
    }
    return info;
  }

  public static CuratorFramework newClient(String connectString) {
    return newClient(connectString, null, null);
  }

  public static CuratorFramework newClient(String connectString, String scheme, String password) {
    RetryPolicy policy = new BoundedExponentialBackoffRetry(1000, 60000, 10);
    CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(connectString).connectionTimeoutMs(5000).sessionTimeoutMs(30000).retryPolicy(policy);
    if (!Strings.isNullOrEmpty(scheme)) {
      builder.authorization(scheme, newBytes(password));
    }
    CuratorFramework client = builder.build();
    client.start();
    return client;
  }

  /**
   * 首先从zookeeper.servers的系统变量,application.properties的配置
   */
  private static CuratorFramework createDefaultClient() {
    Config c = getApplicationConfig();
    String scheme = c.get("zookeeper.authenticationType", "digest");
    String key = "zookeeper.servers";
    String s = System.getProperty(key);
    if (Strings.isNullOrEmpty(s)) {
      s = c.get(key);
    }
    return newClient(s, scheme, c.get("zookeeper.authentication"));
  }

  /**
   * 获取本机内网ip，ip会在第一次访问后缓存起来，并且不会再更新
   * 所以那个模式可能不适合你的机器，本类只是方便大多数人的使用，如果你的
   * 机器不能用该模式获得ip，请使用NetworkInterfaceEx类自行获取
   * @return 返回服务器内部IP
   */
  public static String scanServerInnerIP() {
    try {
      Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
      while (e.hasMoreElements()) {
        NetworkInterface ni = e.nextElement();
        Enumeration<InetAddress> en = ni.getInetAddresses();
        while (en.hasMoreElements()) {
          String ip = en.nextElement().getHostAddress();
          if (isInnerIP(ip)) {
            if (!ip.equals("127.0.0.1") && !ip.equals("0:0:0:0:0:0:0:1")) {
              return ip;
            }
          }
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  /**
   * <pre>
   * 判断一个IP是不是内网IP段的IP
   * 10.0.0.0 – 10.255.255.255
   * 172.16.0.0 – 172.31.255.255
   * 192.168.0.0 – 192.168.255.255
   * </pre>
   * @param ip ip地址
   * @return 如果是内网返回true，否则返回false
   */
  public static boolean isInnerIP(String ip) {
    if (ip == null || ip.length() < 7) {
      return false;
    }

    if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
      return true;
    }

    if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
      return true;
    }

    if (ip.startsWith("172.")) {
      List<String> items = Splitter.on('.').splitToList(ip);
      if (items.size() == 4) {
        int i = Integer.parseInt(items.get(1));
        if (i > 15 && i < 32) {
          return true;
        }
      }
    }
    return false;
  }

  private static class LazyHolder1 {
    private static final String INNER_IP = scanServerInnerIP();
  }


  private static class LazyHolder2 {
    private static final Path CONFIG_PATH = scanConfigPath();
  }


  private static class LazyHolder3 {
    private static final Config CONFIG = applicationConfig();
  }


  private static class LazyHolder4 {
    private static final ProcessInfo PROCESS_INFO = scanProcessInfo();
  }


  private static class LazyHolder5 {
    private static final CuratorFramework DEFAULT_CLIENT = createDefaultClient();
  }
}
