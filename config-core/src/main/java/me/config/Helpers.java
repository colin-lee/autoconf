package me.config;

import com.google.common.base.Splitter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;

/**
 * 基础工具类
 * Created by lirui on 2015-10-01 19:38.
 */
public class Helpers {
  private Helpers() {
  }

  /**
   * 1.扫描配置参数 localConfigPath <br/>
   * 2.扫描类路径下的 autoconf 目录 <br/>
   * 3.如果找不到就用java.io.tmpdir <br/>
   *
   * @return factory
   */
  public static Path scanConfigPath() {
    Path basePath = scanProperty();
    if (basePath != null) {
      return basePath;
    }
    //查找若干文件以便找到classes根目录
    for (String i : Splitter.on(',').split("autoconf,log4j.properties,logback.xml,application.properties")) {
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
    String localCachePath = System.getProperty("localConfigPath");
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
}
