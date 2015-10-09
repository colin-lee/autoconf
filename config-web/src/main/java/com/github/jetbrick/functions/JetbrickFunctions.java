package com.github.jetbrick.functions;

import jetbrick.template.JetAnnotations;
import jetbrick.template.runtime.InterpretContext;
import jetbrick.template.web.JetWebContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 为jetbrick-template提供的一个公共扩展
 *
 * Created by lirui on 2015/1/15.
 */
@JetAnnotations.Functions
public final class JetbrickFunctions {
  public static String copyright() {
    String year = new SimpleDateFormat("yyyy", Locale.CHINA).format(new Date());
    return "©" + year + " ColinLee";
  }

  /**
   * 针对内部url增加contextPath
   *
   * @return 构造好的内部链接
   */
  public static String link(String url) {
    if (url.contains("://") || url.startsWith("//")) {
      return url;
    } else {
      InterpretContext ctx = InterpretContext.current();
      String path = (String) ctx.getValueStack().getValue(JetWebContext.CONTEXT_PATH);
      StringBuilder sb = new StringBuilder(32);
      if (path != null && path.length() > 0) {
        sb.append(path);
      }
      if (url.charAt(0) != '/') {
        sb.append('/');
      }
      sb.append(url);
      return sb.toString();
    }
  }
}
