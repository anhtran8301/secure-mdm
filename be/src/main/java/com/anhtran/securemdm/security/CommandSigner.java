package com.anhtran.securemdm.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Clock;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Signs device commands ({@code typ=mdm-command+jwt}) with the dedicated command key. */
@Component
@RequiredArgsConstructor
public class CommandSigner {
  private static final JOSEObjectType TYPE = new JOSEObjectType("mdm-command+jwt");

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
}
