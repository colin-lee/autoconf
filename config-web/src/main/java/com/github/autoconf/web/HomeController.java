package com.github.autoconf.web;

import com.github.autoconf.entity.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * 业务主程
 * Created by lirui on 2015-10-04 13:35.
 */
@Controller
public class HomeController {
  @RequestMapping("/login")
  public String login(Model model) {
    if (!model.containsAttribute("user")) {
      model.addAttribute("user", new User());
    }
    return "login";
  }

  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public String loginAction(@Valid User user, BindingResult result, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("user", user);
      return "redirect:/login";
    }
    try {
      SecurityUtils.getSubject().login(new UsernamePasswordToken(user.getUsername(), user.getPassword()));
      return "redirect:/";
    } catch (AuthenticationException e) {
      r.addFlashAttribute("message", "用户名或者密码错误");
      r.addFlashAttribute("user", user);
    }
    return "redirect:/login";
  }

  @RequestMapping("/logout")
  public String logout(RedirectAttributes r) {
    SecurityUtils.getSubject().logout();
    r.addFlashAttribute("message", "您已经安全退出");
    return "redirect:/login";
  }

  @RequestMapping("unauthorized")
  public String unauthorized() {
    return "unauthorized";
  }
}
