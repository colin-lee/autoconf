package com.github.autoconf.web;

import com.github.autoconf.entity.User;
import com.github.autoconf.service.UserService;
import com.github.autoconf.shiro.PasswordHelper;
import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * 业务主程
 * Created by lirui on 2015-10-04 13:35.
 */
@Controller
public class UserController {
  @Autowired
  private PasswordHelper passwordHelper;
  @Autowired
  private UserService userService;

  @RequestMapping("/login")
  public String login(Model model) {
    if (!model.containsAttribute("user")) {
      model.addAttribute("user", new User());
    }
    return "user/login";
  }

  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public String loginAction(@Valid User user, BindingResult result, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("message", "用户名或者密码不符合规则");
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

  @RequestMapping("/unauthorized")
  public String unauthorized() {
    return "user/unauthorized";
  }

  @RequestMapping("/ajax/checkLoginName")
  @ResponseBody
  public String checkLoginName(@RequestParam String username) {
    User u = userService.findByUsername(username);
    return u == null ? "true" : "false";
  }

  @RequiresRoles("admin")
  @RequestMapping("/register")
  public String register(Model model) {
    if (!model.containsAttribute("user")) {
      model.addAttribute("user", new User());
    }
    return "user/register";
  }

  @RequiresRoles("admin")
  @RequestMapping(value = "register", method = RequestMethod.POST)
  public String registerAction(@Valid User user, BindingResult result, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("message", "用户名或者密码不符合规则");
      r.addFlashAttribute("user", user);
      return "redirect:/register";
    }
    passwordHelper.encryptPassword(user);
    userService.create(user);
    return "redirect:/profile/?username=" + user.getUsername();
  }

  @RequestMapping("/profile")
  public String profile(@RequestParam(required = false) String username, Model model) {
    if (!model.containsAttribute("user")) {
      if (Strings.isNullOrEmpty(username)) {
        username = SecurityUtils.getSubject().getPrincipal().toString();
      }
      User user = userService.findByUsername(username);
      model.addAttribute("user", user);
    }
    return "user/profile";
  }

  @RequiresRoles("admin")
  @RequestMapping(value = "/profile", method = RequestMethod.POST)
  public String profileAction(@Valid User user, RedirectAttributes r) {
    userService.updateAuthentication(user);
    r.addFlashAttribute("message", "更新用户权限成功");
    return "redirect:/profile/?username=" + user.getUsername();
  }

  @RequestMapping("/password")
  public String password(@RequestParam(required = false) String username, Model model) {
    if (!model.containsAttribute("user")) {
      if (Strings.isNullOrEmpty(username)) {
        username = SecurityUtils.getSubject().getPrincipal().toString();
      }
      User user = userService.findByUsername(username);
      model.addAttribute("user", user);
    }
    return "user/password";
  }

  @RequestMapping(value = "/password", method = RequestMethod.POST)
  public String passwordAction(User user, RedirectAttributes r) {
    passwordHelper.encryptPassword(user);
    userService.updatePassword(user);
    r.addFlashAttribute("message", "修改用户密码成功");
    return "redirect:/profile/?username=" + user.getUsername();
  }
}
