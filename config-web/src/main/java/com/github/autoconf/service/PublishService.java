package com.github.autoconf.service;

import com.github.autoconf.entity.Config;
import com.github.autoconf.entity.ConfigHistory;
import com.github.autoconf.helper.ConfigHelper;
import com.github.autoconf.helper.ZookeeperUtil;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 异步发布配置到zookeeper服务器
 * <p/>
 * Created by lirui on 15/2/9.
 */
@Service
public class PublishService {
  private Logger log = LoggerFactory.getLogger(PublishService.class);
  @Autowired
  private ConfigService service;
  @Autowired
  private ConfigHistoryService history;
  @Value("${zookeeper.authenticationType}")
  private String authType = "digest";
  @Value("${zookeeper.authentication}")
  private String auth = "root:Password4Zookeeper";
  private CuratorFramework client;
  private List<ACL> defaultAclList = Lists.newArrayList();

  @PostConstruct
  void init() {
    client = ConfigHelper.getDefaultClient();
    HashCode hash = Hashing.sha1().newHasher().putString(auth, Charsets.UTF_8).hash();
    String encoded = new BASE64Encoder().encode(hash.asBytes());
    int pos = auth.indexOf(':');
    if (pos == -1) {
      return;
    }
    String user = auth.substring(0, pos);
    List<ACL> acl = Lists.newArrayList();
    acl.add(new ACL(ZooDefs.Perms.READ, new Id("world", "anyone")));
    acl.add(new ACL(ZooDefs.Perms.ALL, new Id(authType, user + ':' + encoded)));
    defaultAclList = acl;
  }

  /**
   * 发布配置，需要：备份原配置，保存新内容，发布到zookeeper。
   */
  public void cpZookeeper(Config config, boolean scp) {
    clearCache();

    // 首先保存config_history备份
    Config old = service.findById(config.getId());
    ConfigHistory backup = new ConfigHistory();
    backup.copy(old);
    try {
      history.insert(backup);
    } catch (Exception e) {
      log.error("cannot insertBackup({})", backup, e);
    }

    if (!scp)
      return;

    try {
      // 发布到zookeeper中
      byte[] payload;
      if ("gbk".equalsIgnoreCase(config.getEncoding())) {
        payload = ZookeeperUtil.newBytes(config.getContent(), ZookeeperUtil.GBK);
      } else {
        payload = ZookeeperUtil.newBytes(config.getContent());
      }
      ZookeeperUtil.ensure(client, config.getPath());
      ZookeeperUtil.setData(client, config.getPath(), payload);
      // 设定路径权限
      if (!Iterables.isEmpty(defaultAclList)) {
        ZookeeperUtil.setACL(client, config.getPath(), defaultAclList);
      }
    } catch (Exception e) {
      log.error("cannot publish to zookeeper, path={}", config.getPath(), e);
      throw new RuntimeException("cannot publish to zookeeper, path=" + config.getPath() + ", " + e.getMessage());
    }
  }

  /**
   * 删除配置，把内容清空，存到history中，从zookeeper删除对应内容
   */
  public void rmZookeeper(Config config) {
    clearCache();

    // 首先保存config_history备份
    ConfigHistory backup = new ConfigHistory();
    backup.copy(config);
    try {
      history.insert(backup);
    } catch (Exception e) {
      log.error("cannot insertBackup({})", backup, e);
    }

    try {
      // 从zookeeper中删除，递归删除空路径
      String path = config.getPath();
      while (true) {
        List<String> children = ZookeeperUtil.getChildren(client, path);
        if (children == null || children.size() == 0) {
          client.delete().forPath(path);
          log.info("deletePath({})", path);
          int index = path.lastIndexOf('/');
          if (index == -1)
            return;
          path = path.substring(0, index);
          byte[] data = ZookeeperUtil.getData(client, path);
          if (data != null && data.length > 0) {
            log.info("{} is not Empty, now Exit", path);
            break;
          }
        } else {
          return;
        }
      }
    } catch (KeeperException.NoNodeException ignored) {
    } catch (Exception e) {
      log.error("cannot delete from Zookeeper, path={}", config.getPath(), e);
      throw new RuntimeException("cannot delete from zookeeper, path=" + config.getPath() + ", " + e.getMessage());
    }
  }

  private void clearCache() {
    history.clearCache();
    service.clearCache();
  }
}
