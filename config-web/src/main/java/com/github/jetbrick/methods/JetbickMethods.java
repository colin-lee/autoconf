package com.github.jetbrick.methods;

import com.github.jetbrick.functions.JetbrickFunctions;
import jetbrick.template.JetAnnotations;
import jetbrick.util.StringEscapeUtils;

/**
 * 为已有java类型添加扩展功能，类似于js修改prototype
 *
 * Created by lirui on 2015/1/15.
 */
@JetAnnotations.Methods
public final class JetbickMethods {
  /**
   * 构造链接，会对title做escape避免xss漏洞。<br/>
   * 潜规则：只有外部链接才带 http:// 开头，这个时候自动增加 target=_blank
   *
   * @param title      链接标题
   * @param url        url
   * @param attributes 可选属性，以"#"开头的是id，以"."开头的是class，其他的会原样输出
   * @return 构造好的a标签
   */
  public static String link(String title, String url, String... attributes) {
    StringBuilder sb = new StringBuilder(128);
    sb.append("<a href=\"");
    if (url.contains("://") || url.startsWith("//")) {
      sb.append(url).append("\" target=\"_blank\"");
    } else {
      sb.append(JetbrickFunctions.link(url)).append('"');
    }
    if (attributes != null && attributes.length > 0) {
      for (String i : attributes) {
        if (i.length() == 0)
          continue;
        char firstChar = i.charAt(0);
        if (firstChar == '#') {
          sb.append(" id=\"").append(i.substring(1)).append('"');
        } else if (firstChar == '.') {
          sb.append(" class=\"").append(i.substring(1)).append('"');
        } else {
          sb.append(' ').append(i);
        }
      }
    }
    sb.append('>').append(StringEscapeUtils.escapeXml(title)).append("</a>");
    return sb.toString();
  }
}
