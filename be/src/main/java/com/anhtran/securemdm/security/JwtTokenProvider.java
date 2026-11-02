package com.anhtran.securemdm.security;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.utils.AppConstants;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Issues and verifies short-lived ES256 admin access tokens ({@code typ=at+jwt}). */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
  static final String ISSUER = "secure-mdm";
  private static final String AUDIENCE = "secure-mdm-admin";
  private static final JOSEObjectType TYPE = new JOSEObjectType("at+jwt");
  private static final long CLOCK_SKEW_MS = 30_000;

  private final SigningKeys keys;
  private final Clock clock;

  static String sign(ECKey key, JOSEObjectType type, JWTClaimsSet claims) {
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256).type(type).keyID(key.getKeyID()).build(),
              claims);
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (Exception ex) {
      throw new IllegalStateException("Signing failed", ex);
    }
  }

  public String issue(Actor actor) {
    long now = clock.millis();
    return sign(
        keys.getAccessKey(),
        TYPE,
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .subject(actor.id())
            .claim("sid", actor.sessionId())
            .claim("tenant", actor.organizationId())
            .issueTime(new Date(now))
            .expirationTime(new Date(now + AppConstants.ACCESS_TOKEN_TTL_MS))
            .build());
  }

  /** Strict algorithm, type, key and claim checks; any failure is a 401. */
  public JWTClaimsSet verify(String token) {
    try {
      ECKey key = keys.getAccessKey();
      SignedJWT jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())
          || !TYPE.equals(jwt.getHeader().getType())
          || !Objects.equals(key.getKeyID(), jwt.getHeader().getKeyID())
          || !jwt.verify(new ECDSAVerifier(key.toPublicJWK()))) {
        throw CommonException.unauthorized();
      }
      var claims = jwt.getJWTClaimsSet();
      long now = clock.millis();
      if (!ISSUER.equals(claims.getIssuer())
          || !claims.getAudience().equals(List.of(AUDIENCE))
          || claims.getExpirationTime() == null
          || claims.getExpirationTime().getTime() <= now
          || claims.getIssueTime() == null
          || claims.getIssueTime().getTime() > now + CLOCK_SKEW_MS) {
        throw CommonException.unauthorized();
      }
      return claims;
    } catch (Exception ex) {
      throw CommonException.unauthorized();
    }
  }
}
