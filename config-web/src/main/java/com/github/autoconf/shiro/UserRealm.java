package com.github.autoconf.shiro;

import com.github.autoconf.entity.User;
import com.github.autoconf.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户身份及权限认证
 * Created by lirui on 2015-10-04 18:13.
 */
public class UserRealm extends AuthorizingRealm {
  @Autowired
  private UserService userService;

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = (String) principals.getPrimaryPrincipal();
    SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    User user = userService.findByUsername(username);
    authorizationInfo.setRoles(user.getRoleSet());
    authorizationInfo.setStringPermissions(user.getPermissionSet());
    return authorizationInfo;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    String username = (String) token.getPrincipal();
    User user = userService.findByUsername(username);
    if (user == null) {
      throw new UnknownAccountException();//没找到帐号
    }
    if (Boolean.TRUE.equals(user.isLocked())) {
      throw new LockedAccountException(); //帐号锁定
    }
    //更新用户最后登录时间
    userService.updateLoginTime(user.getUsername());
    //交给AuthenticatingRealm使用CredentialsMatcher进行密码匹配，如果觉得人家的不好可以自定义实现
    return new SimpleAuthenticationInfo(user.getUsername(), //用户名
      user.getPassword(), //密码
      ByteSource.Util.bytes(user.getCredentialsSalt()),//salt=username+salt
      getName()  //realm name
    );
  }
}
