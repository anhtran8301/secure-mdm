package com.anhtran.securemdm.src.identity.config;

import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.security.Role;
import com.anhtran.securemdm.src.identity.entity.Organization;
import com.anhtran.securemdm.src.identity.entity.User;
import com.anhtran.securemdm.src.identity.repository.OrganizationRepository;
import com.anhtran.securemdm.src.identity.repository.UserRepository;
import com.anhtran.securemdm.utils.IdUtils;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Seeds organizations alpha/beta, each with org_admin, operator and viewer @demo.local users. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {
  private static final List<Role> ROLES = List.of(Role.ORG_ADMIN, Role.OPERATOR, Role.VIEWER);

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final MdmProperties properties;

  @Override
  public void run(ApplicationArguments args) {
    String password = properties.demoPassword();
    if (password == null || password.length() < 12) {
      throw new IllegalArgumentException("DEMO_PASSWORD must be at least 12 characters");
    }
    String hash = passwordEncoder.encode(password);
    // ApplicationRunner executes on the startup thread, never a request event loop.
    Flux.just(
            organization("00000000-0000-0000-0000-000000000001", "alpha"),
            organization("00000000-0000-0000-0000-000000000002", "beta"))
        .concatMap(
            org ->
                organizationRepository
                    .insertIfAbsent(org)
                    .thenMany(
                        Flux.fromIterable(ROLES)
                            .concatMap(
                                role -> userRepository.upsertPassword(user(org, role, hash)))))
        .then()
        .block(Duration.ofSeconds(30));
  }

  private static Organization organization(String id, String slug) {
    return Organization.builder().id(id).slug(slug).name("Organization " + slug).build();
  }

  private static User user(Organization org, Role role, String hash) {
    return User.builder()
        .id(IdUtils.newId())
        .organizationId(org.getId())
        .email(role.name().toLowerCase(Locale.ROOT) + "@demo.local")
        .passwordHash(hash)
        .role(role.name())
        .build();
  }
}
