package com.github.autoconf.web;

import com.github.autoconf.entity.Config;
import com.github.autoconf.entity.ConfigHistory;
import com.github.autoconf.entity.ReplaceRequest;
import com.github.autoconf.service.ConfigHistoryService;
import com.github.autoconf.service.ConfigService;
import com.github.autoconf.service.ZookeeperService;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Date;
import java.util.List;
import java.util.Set;

/**
 * 创建/修改/删除等操作
 *
 * Created by lirui on 2015/2/8.
 */
@Controller
public class AdminController {
  private Logger log = LoggerFactory.getLogger(AdminController.class);

  @Autowired
  private ConfigService configService;
  @Autowired
  private ConfigHistoryService historyService;
  @Autowired
  private ZookeeperService zookeeperService;

  @RequestMapping(value = "/create/config/", method = RequestMethod.GET)
  public String createConfig() {
    return "form_config_create";
  }

  @RequestMapping(value = "/create/config/", method = RequestMethod.POST)
  public String createConfigAction(@Valid Config c, BindingResult bind, RedirectAttributes r) {
    if (bind.hasErrors()) {
      FieldError error = bind.getFieldError();
      String msg = error.getField() + " 错误: " + error.getDefaultMessage();
      r.addFlashAttribute("message", msg);
      r.addFlashAttribute("config", c);
      return "redirect:/create/config/";
    }
    c.setModifyTime(new Date(System.currentTimeMillis()));
    c.setEditor(getCurrentUserName());
    CharMatcher matcher = CharMatcher.anyOf(",; |");
    try {
      if (matcher.matchesAnyOf(c.getProfile())) {
        // 批量创建
        for (String i : Splitter.on(matcher).trimResults().omitEmptyStrings().split(c.getProfile())) {
          Config one = new Config();
          one.setEditor(getCurrentUserName());
          one.setName(c.getName());
          one.setProfile(i);
          one.setContent(c.getContent());
          configService.save(one);
          zookeeperService.cpZookeeper(one);
        }
      } else {
        configService.save(c);
        zookeeperService.cpZookeeper(c);
      }
      return "redirect:/?search=" + encodeURL(c.getName());
    } catch (Exception e) {
      log.error("/create/config/: {}", c, getFirstNotNullMessage(e));
      r.addFlashAttribute("message", "创建失败！原因：" + e.getMessage());
      r.addFlashAttribute("config", c);
      return "redirect:/create/config/";
    }
  }

  @RequestMapping(value = "/edit/config/{id}", method = RequestMethod.GET)
  public String editConfig(@PathVariable long id, Model m, RedirectAttributes r) {
    if (!r.getFlashAttributes().containsKey("config")) {
      Config c = configService.findById(id);
      if (c == null) {
        r.addFlashAttribute("message", "找不到id为" + id + "的配置");
        return "redirect:/";
      }
      m.addAttribute("config", c);
    }
    return "form_config_edit";
  }

  @RequestMapping(value = "/delete/config/{id}", method = RequestMethod.GET)
  public String deleteConfig(@PathVariable long id, RedirectAttributes r) {
    Config c = configService.findById(id);
    String url = "/";
    try {
      if (c != null) {
        c.setEditor(getCurrentUserName());
        configService.delete(c);
        zookeeperService.rmZookeeper(c);
        r.addFlashAttribute("message", "删除id为" + id + "的配置成功！");
        url = "/?search=" + encodeURL(c.getName());
      }
    } catch (Exception e) {
      r.addFlashAttribute("message", "删除id为" + id + "的配置失败！原因：" + getFirstNotNullMessage(e));
    }
    return "redirect:" + url;
  }

  @RequestMapping(value = "/edit/config/", method = RequestMethod.POST)
  public String editConfigAction(@ModelAttribute("config") Config c, RedirectAttributes r) {
    try {
      c.setEditor(getCurrentUserName());
      configService.save(c);
      zookeeperService.cpZookeeper(c);
      r.addFlashAttribute("message", "修改成功");
    } catch (Exception e) {
      log.error("/edit/config/: {}", c, e);
      r.addFlashAttribute("config", c);
      r.addFlashAttribute("message", "修改失败！原因：" + getFirstNotNullMessage(e));
    }
    return "redirect:/edit/config/" + c.getId();
  }

  @RequestMapping(value = "/recover/history/", method = RequestMethod.POST)
  public String recoverHistory(@RequestParam long id, RedirectAttributes r) {
    ConfigHistory h = historyService.findbyId(id);
    if (h == null) {
      r.addFlashAttribute("message", "找不到id为" + id + "的配置历史");
      return "redirect:/";
    }
    Config c = configService.findById(h.getConfigId());
    if (c == null) {
      r.addFlashAttribute("message", "找不到id为" + h.getConfigId() + "的配置");
      return "redirect:/";
    }
    try {
      c.setEditor(getCurrentUserName());
      c.setContent(h.getContent());
      configService.save(c);
      zookeeperService.cpZookeeper(c);
      r.addFlashAttribute("message", "回滚版本为" + h.getVersion() + "的配置\"" + c.getName() + "\"成功");
    } catch (Exception e) {
      log.error("/recover/history/: {}", h, e);
      r.addFlashAttribute("history", h);
      r.addFlashAttribute("message", "回滚失败！原因：" + getFirstNotNullMessage(e));
      return "redirect:/view/history/" + h.getId();
    }
    return "redirect:/edit/config/" + c.getId();
  }

  @ModelAttribute("config")
  public Config findById(@RequestParam(required = false) Long id) {
    Config c = null;
    if (id != null && id > 0) {
      c = configService.findById(id);
    }
    if (c == null) {
      c = new Config();
    }
    return c;
  }

  @RequestMapping("/replace/form/")
  public String replaceConfigForm() {
    return "form_config_replace";
  }

  @RequestMapping(value = "/edit/copy/{id}")
  public String createFromCopy(@PathVariable long id, RedirectAttributes r) {
    Config c = configService.findById(id);
    c.setId(null);
    r.addFlashAttribute("config", c);
    return "redirect:/create/config/";
  }

  @RequestMapping(value = "/replace/content/", method = RequestMethod.POST)
  public String replaceConfigContent(@Valid ReplaceRequest req, BindingResult result, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("message", result.getAllErrors());
      r.addFlashAttribute("replace", req);
      return "redirect:/replace/form/";
    }
    Set<Long> ids = Sets.newHashSet(req.getConfigIds());
    try {
      for (Config c : configService.findAll()) {
        if (!ids.contains(c.getId())) {
          continue;
        }
        if (c.getContent().contains(req.getSrc())) {
          c.setEditor(getCurrentUserName());
          c.setContent(StringUtils.replace(c.getContent(), req.getSrc(), req.getDst()));
          configService.save(c);
          zookeeperService.cpZookeeper(c);
          log.info("{}(profile={}) replace [{}] -> [{}]", c.getName(), c.getProfile(), req.getSrc(), req.getDst());
        }
      }
    } catch (Exception e) {
      log.error("/replace/content/: {}", req, e);
      r.addFlashAttribute("message", "修改失败！原因：" + e.getMessage());
      r.addFlashAttribute("replace", req);
      return "redirect:/replace/form/";
    }
    return "redirect:/?search=" + encodeURL(req.getDst());
  }

  /**
   * 预览修改前后对比，并选择真正要修改的文件
   */
  @RequestMapping("/replace/preview/")
  public String replaceContextPreview(@Valid ReplaceRequest req, BindingResult result, Model model, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("message", result.getAllErrors());
      r.addFlashAttribute("replace", req);
      return "redirect:/replace/form/";
    }
    List<ReplaceRequest> affected = Lists.newArrayList();
    for (Config i : configService.findAll()) {
      if (i.getContent().contains(req.getSrc())) {
        affected.add(preview(req, i));
      }
    }
    model.addAttribute("replace", req);
    model.addAttribute("affected", affected);
    return "replace_preview";
  }

  private ReplaceRequest preview(ReplaceRequest req, Config config) {
    List<String> oldLines = Lists.newArrayList(), newLines = Lists.newArrayList();
    List<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(config.getContent());
    for (String i : lines) {
      if (i.contains(req.getSrc())) {
        oldLines.add(highlight(i, req.getSrc()));
        newLines.add(highlight(StringUtils.replace(i, req.getSrc(), req.getDst()), req.getDst()));
      }
    }
    if (oldLines.size() > 0 && newLines.size() > 0) {
      ReplaceRequest one = new ReplaceRequest();
      one.setConfig(config);
      one.setOldLines(Joiner.on("<br/>").join(oldLines));
      one.setNewLines(Joiner.on("<br/>").join(newLines));
      return one;
    }
    return null;
  }

  private String highlight(String raw, String needle) {
    StringBuilder sb = new StringBuilder();
    int start = 0;
    int stop = raw.indexOf(needle, start);
    while (stop != -1) {
      sb.append(HtmlEscapers.htmlEscaper().escape(raw.substring(start, stop)));
      sb.append("<span class=\"bg-danger text-blue\">").append(needle).append("</span>");
      start = stop + needle.length();
      stop = raw.indexOf(needle, start);
    }
    if (start < raw.length()) {
      sb.append(HtmlEscapers.htmlEscaper().escape(raw.substring(start)));
    }
    return sb.toString();
  }

  public String getFirstNotNullMessage(Throwable e) {
    if (e == null)
      return null;
    String msg = e.getMessage();
    if (!Strings.isNullOrEmpty(msg))
      return msg;
    return getFirstNotNullMessage(e.getCause());
  }

  private String getCurrentUserName() {
    return (String) SecurityUtils.getSubject().getPrincipal();
  }

  private String encodeURL(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return s;
    }
  }
}
