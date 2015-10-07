package com.github.autoconf.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 业务处理
 * Created by lirui on 2015-10-07 21:28.
 */
@Controller
public class AppController {
  @RequestMapping(value = {"/", "/home"})
  public String home() {
    return "home";
  }
}
