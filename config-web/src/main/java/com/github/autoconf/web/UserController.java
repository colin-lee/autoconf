package com.github.autoconf.web;

import com.github.autoconf.entity.User;
import com.github.autoconf.service.UserService;
import com.github.autoconf.shiro.PasswordHelper;
import com.github.jetbrick.functions.ShiroFunctions;
import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import java.util.List;

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

  @RequestMapping("/admin/register")
  public String register(Model model) {
    if (!model.containsAttribute("user")) {
      model.addAttribute("user", new User());
    }
    return "user/register";
  }

  @RequestMapping(value = "/admin/register", method = RequestMethod.POST)
  public String registerAction(@Valid User user, BindingResult result, RedirectAttributes r) {
    if (result.hasErrors()) {
      r.addFlashAttribute("message", "用户名或者密码不符合规则");
      r.addFlashAttribute("user", user);
      return "redirect:/admin/register";
    }
    passwordHelper.encryptPassword(user);
    userService.create(user);
    return "redirect:/admin/profile/?username=" + user.getUsername();
  }

  @RequestMapping("/admin/profile")
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

  @RequestMapping(value = "/admin/profile", method = RequestMethod.POST)
  public String profileAction(@Valid User user, RedirectAttributes r) {
    userService.updateAuthentication(user);
    r.addFlashAttribute("message", "更新用户权限成功");
    return "redirect:/admin/profile/?username=" + user.getUsername();
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
    String username = user.getUsername();
    //只有自己和管理员能修改密码
    if (ShiroFunctions.hasRole("admin") || username.equals(ShiroFunctions.principal())) {
      passwordHelper.encryptPassword(user);
      userService.updatePassword(user);
      r.addFlashAttribute("message", "修改用户密码成功");
    } else {
      r.addFlashAttribute("message", "抱歉,您没有权限");
    }
    return "redirect:/admin/profile/?username=" + username;
  }

  @RequestMapping("/admin/user")
  public String userList(Model model) {
    DateTimeFormatter df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    List<User> users = userService.findAll();
    StringBuilder sbd = new StringBuilder(1024);
    sbd.append('[');
    for (User u : users) {
      sbd.append('[');
      sbd.append('"').append(u.getUsername()).append('"').append(',');
      sbd.append('"').append(u.getRoles()).append('"').append(',');
      sbd.append('"').append(u.getPermissions()).append('"').append(',');
      sbd.append('"').append(u.isLocked() ? "是" : "否").append('"').append(',');
      sbd.append('"').append(df.print(u.getLastLogin().getTime())).append('"');
      sbd.append(']').append(',');
    }
    sbd.append(']');
    model.addAttribute("data", sbd.toString());
    return "user/list";
  }

  @RequestMapping("/admin/lock")
  public String lock(@RequestParam String username, RedirectAttributes r) {
    userService.lock(username);
    r.addFlashAttribute("message", "锁定" + username + "成功");
    return "redirect:/admin/user";
  }
}
