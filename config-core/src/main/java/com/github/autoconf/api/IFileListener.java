package com.github.autoconf.api;

import java.nio.file.Path;

/**
 * 文件变更通知
 * Created by lirui on 2015-09-30 15:05.
 */
public interface IFileListener {
  /**
   * 文件修改通知
   *
   * @param path    文件路径
   * @param content 文件内容
   */
  void changed(Path path, byte[] content);
}
