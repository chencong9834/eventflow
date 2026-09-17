package com.example.eventflow.identity;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedPasswordHasher implements ApplicationRunner {

  private final SysUserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public SeedPasswordHasher(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    var pending = userMapper.findByPasswordHash("SEED_PLAIN");
    if (pending.isEmpty()) {
      return;
    }
    String hash = passwordEncoder.encode(AuthService.DEMO_PASSWORD);
    pending.forEach(user -> userMapper.updatePasswordHash(user.getId(), hash));
  }
}
