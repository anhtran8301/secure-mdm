package com.anhtran.securemdm.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Signs device commands ({@code typ=mdm-command+jwt}) with the dedicated command key. */
@Component
@RequiredArgsConstructor
public class CommandSigner {
  private static final JOSEObjectType TYPE = new JOSEObjectType("mdm-command+jwt");
  private static final JOSEObjectType TRUST_TYPE = new JOSEObjectType("mdm-trust+jwt");
  private static final long TRUST_TTL_MS = 86_400_000L;

  private final SigningKeys keys;
  private final Clock clock;

  public String sign(
      String commandId,
      String organizationId,
      String deviceId,
      String type,
      Map<String, Object> payload,
      long expiresAt) {
    return JwtTokenProvider.sign(
        keys.getCommandKey(),
        TYPE,
        new JWTClaimsSet.Builder()
            .issuer(JwtTokenProvider.ISSUER)
            .audience(deviceId)
            .jwtID(commandId)
            .claim("tenant", organizationId)
            .claim("commandType", type)
            .claim("payload", payload)
            .issueTime(new Date(clock.millis()))
            .expirationTime(new Date(expiresAt))
            .build());
  }

  /**
   * The published command keys as one Trust Bundle per published key, all with the same payload, so
   * a device that trusts any one of them can learn the rest (ADR 0006).
   */
  public List<String> trustBundles() {
    long now = clock.millis();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(JwtTokenProvider.ISSUER)
            .claim(
                "keys",
                new JWKSet(keys.getCommandKeys().stream().map(JWK::toPublicJWK).toList())
                    .toJSONObject()
                    .get("keys"))
            .issueTime(new Date(now))
            .expirationTime(new Date(now + TRUST_TTL_MS))
            .build();
    return keys.getCommandKeys().stream()
        .map(key -> JwtTokenProvider.sign(key, TRUST_TYPE, claims))
        .toList();
  }
}
