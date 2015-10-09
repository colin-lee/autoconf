package com.github.autoconf.web;

import com.github.autoconf.entity.ConfigHistory;
import com.github.autoconf.service.ConfigHistoryService;
import com.github.autoconf.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 默认页面
 * Created by lirui on 2014/10/13.
 */
@Controller
public class AppController {
  @Autowired
  private ConfigHistoryService historyService;
  @Autowired
  private ConfigService configService;

  @RequestMapping(value = {"/", "/home"})
  public String home() {
    return "home";
  }

  @RequestMapping("/history")
  public String recent(HttpServletRequest request, Model model) {
    String ajax = "ajax/history/";
    if (request.getQueryString() != null) {
      ajax += '?' + request.getQueryString();
    }
    model.addAttribute("ajax", ajax);
    return "history";
  }

  @RequestMapping("/view/history/{id}")
  public String viewHistory(@PathVariable long id, Model model) {
    ConfigHistory h = historyService.findbyId(id);
    model.addAttribute("history", h);
    model.addAttribute("configExist", configService.findById(h.getConfigId()) != null);
    return "view_history";
  }
}
