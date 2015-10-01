package me.config.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;
import java.util.Collection;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static me.config.helper.JmxHelper.*;

/**
 * 判断提供服务的容器
 * <p/>
 * Created by lirui on 2015/04/17 上午11:08.
 */
public class WebServer {
  private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);

  public static boolean isTomcat() {
    return mbeanExists("Catalina:type=Server");
  }

  public static boolean isJBoss() {
    return mbeanExists("jboss.system:type=Server");
  }

  public static boolean isJetty() {
    return mbeanExists("org.mortbay.jetty:type=server,id=0");
  }

  public static boolean isJetty5() {
    return mbeanExists("org.mortbay:jetty=default");
  }

  public static boolean isResin() {
    return mbeanExists("resin:type=Resin");
  }

  public static boolean isGlassfish() {
    return mbeanExists("amx:pp=,type=domain-root")
      || mbeanExists("com.sun.appserv:j2eeType=J2EEServer,category=runtime,*");
  }

  public static Integer getTomcatHttpPort() throws Exception {
    Collection<ObjectName> connectors = getSelectors("*:type=Connector,*");

    int lowest = MAX_VALUE;
    for (final ObjectName connector : connectors) {
      try {
        String protocol = queryString(connector, "protocol");
        Integer port = queryInt(connector, "port");
        LOG.info("tomcat, protocol={}, port={}", protocol, port);
        if (protocol != null && protocol.toLowerCase().contains("http")) {
          if (port == null)
            continue;
          lowest = Math.min(lowest, port);
        }
      } catch (AttributeNotFoundException e) {
        // quietly skip this connector, it's probably the wrong kind
      }
    }

    // maybe there are no HTTP connectors?
    if (lowest == MAX_VALUE) {
      for (final ObjectName connector : connectors) {
        try {
          lowest = Math.min(lowest, queryInt(connector, "port"));
        } catch (AttributeNotFoundException e) {
          // quietly skip this connector, it's probably the wrong kind
        }
      }
    }

    if (lowest == MAX_VALUE) {
      return null;
    }

    return lowest;
  }

  public static Integer getResinHttpPort() throws Exception {
    Collection<ObjectName> selectors = getSelectors("resin:type=Port,*");
    int highestHttp = -1, highestOther = -1;
    for (final ObjectName selector : selectors) {
      final Integer port = queryInt(selector, "Port");
      if (port == null)
        continue;
      String protocol = queryString(selector, "ProtocolName");
      String address = queryString(selector, "Address");
      LOG.info("resin, protocol={}\taddress={}\tport={}", protocol, address, port);
      if (protocol == null || !"http".equalsIgnoreCase(protocol.trim())) {
        highestOther = max(highestOther, port.intValue());
      } else {
        highestHttp = max(highestHttp, port.intValue());
      }
    }

    // maybe there are no HTTP connectors?
    if (highestHttp != -1) {
      return highestHttp;
    } else if (highestOther != -1) {
      return highestOther;
    } else {
      return null;
    }
  }

  public static Integer getJettyHttpPort() throws Exception {
    Collection<ObjectName> selectors =
      getSelectors("org.mortbay.jetty.nio:type=selectchannelconnector,*");
    int lowest = MAX_VALUE;
    for (final ObjectName selector : selectors) {
      lowest = min(lowest, queryInt(selector, "port"));
    }

    if (lowest == MAX_VALUE) {
      return null;
    }

    return lowest;
  }

  public static Integer getJetty5HttpPort() throws Exception {
    Collection<ObjectName> selectors = getSelectors("org.mortbay:jetty=default,*");
    int lowest = MAX_VALUE;
    for (final ObjectName selector : selectors) {
      if (selector.toString().matches("org.mortbay:jetty=default,SocketListener=[0=9]"))
        lowest = min(lowest, queryInt(selector, "port"));
    }

    if (lowest == MAX_VALUE) {
      return null;
    }

    return lowest;
  }

  public static Integer getGlassfishHttpPort() throws Exception {
    Collection<ObjectName> selectors = getSelectors("com.sun.appserv:type=Selector,*");
    int lowest = MAX_VALUE;
    for (final ObjectName selector : selectors) {
      final String name = selector.toString();
      if (name.contains("http")) {
        lowest = min(lowest, parseInt(name.replaceAll("[^0-9]", "")));
      }
    }

    // maybe there are no HTTP connectors?
    if (lowest == MAX_VALUE) {
      for (final ObjectName selector : selectors) {
        final String name = selector.toString();
        lowest = min(lowest, parseInt(name.replaceAll("[^0-9]", "")));
      }
    }

    if (lowest == MAX_VALUE) {
      return null;
    }
    return lowest;
  }

  public static Collection<ObjectName> getSelectors(String name) throws Exception {
    Collection<ObjectName> selectors;
    for (int tries = 1; tries <= 30; tries++) {
      selectors = queryNames(name);
      if (selectors.isEmpty()) {
        sleep(1000L);
        LOG.info("query({}), try {} times, waiting {} seconds", name, tries, tries);
      } else {
        return selectors;
      }
    }
    throw new IllegalStateException(
      name + ", selector MBeans were not loaded after 30 seconds, aborting");
  }

  /**
   * 根据协议和scheme获取服务端口号
   *
   * @return 端口号
   */
  public static Integer getHttpPort() throws Exception {
    if (WebServer.isTomcat() || WebServer.isJBoss()) {
      return WebServer.getTomcatHttpPort();
    }
    if (WebServer.isResin()) {
      return WebServer.getResinHttpPort();
    }
    if (WebServer.isJetty()) {
      return WebServer.getResinHttpPort();
    }
    if (WebServer.isJetty5()) {
      return WebServer.getJetty5HttpPort();
    }
    if (WebServer.isGlassfish()) {
      return WebServer.getGlassfishHttpPort();
    }
    LOG.error("cannot detect ContainerType");
    return null;
  }
}
