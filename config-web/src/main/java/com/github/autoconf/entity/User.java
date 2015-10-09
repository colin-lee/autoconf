package com.github.autoconf.entity;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.Set;

/**
 * 用户
 * Created by lirui on 2015-10-04 18:20.
 */
public class User {
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
  private Long id;
  @NotNull
  @Pattern(regexp = "[0-9a-zA-Z_-]+")
  private String username;
  private String password;
  private String salt;
  private String roles = "";
  private String permissions = "";
  private Date lastLogin;
  private boolean locked;

  public User() {
  }

  public User(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  public String getCredentialsSalt() {
    return username + salt;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }

  public String getPermissions() {
    return permissions;
  }

  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }

  public Set<String> getRoleSet() {
    String s = roles == null ? "" : roles;
    return Sets.newLinkedHashSet(SPLITTER.split(s));
  }

  public Set<String> getPermissionSet() {
    String s = permissions == null ? "" : permissions;
    return Sets.newLinkedHashSet(SPLITTER.split(s));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    User user = (User) o;
    return !(id != null ? !id.equals(user.id) : user.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "User{" +
      "id=" + id +
      ", username='" + username + '\'' +
      ", locked=" + locked +
      '}';
  }
}
