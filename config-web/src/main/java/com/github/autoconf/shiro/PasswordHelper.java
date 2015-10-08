package com.github.autoconf.shiro;

import com.github.autoconf.entity.User;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Value;

public class PasswordHelper {
  private RandomNumberGenerator randomNumberGenerator = new SecureRandomNumberGenerator();

  @Value("${shiro.password.algorithmName}")
  private String algorithmName = "md5";
  @Value("${shiro.password.hashIterations}")
  private int hashIterations = 2;

  public void setRandomNumberGenerator(RandomNumberGenerator randomNumberGenerator) {
    this.randomNumberGenerator = randomNumberGenerator;
  }

  public void setAlgorithmName(String algorithmName) {
    this.algorithmName = algorithmName;
  }

  public void setHashIterations(int hashIterations) {
    this.hashIterations = hashIterations;
  }

  public void encryptPassword(User user) {

    user.setSalt(randomNumberGenerator.nextBytes().toHex());

    String newPassword = new SimpleHash(algorithmName, user.getPassword(), ByteSource.Util.bytes(user.getCredentialsSalt()), hashIterations).toHex();

    user.setPassword(newPassword);
  }
}
