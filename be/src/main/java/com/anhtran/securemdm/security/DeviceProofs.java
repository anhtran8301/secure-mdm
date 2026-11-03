package com.anhtran.securemdm.security;

import com.anhtran.securemdm.exception.CommonException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Device Proof checks that need no database (ADR 0005): type, algorithm, signature with the stored
 * device key, method, target and the ±60 s issue-time window. Replay is checked by the caller.
 */
public final class DeviceProofs {
  public static final long WINDOW_MS = 60_000L;
  private static final JOSEObjectType TYPE = new JOSEObjectType("mdm-device-proof+jwt");
  private static final int MAX_JTI = 64;

  private DeviceProofs() {}

  /** Parses the proof and checks its header; the {@code kid} is the certificate serial. */
  public static SignedJWT parse(String proof) {
    try {
      SignedJWT jwt = SignedJWT.parse(proof);
      var header = jwt.getHeader();
      if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())
          || !TYPE.equals(header.getType())
          || header.getKeyID() == null
          || header.getKeyID().isBlank()) {
        throw CommonException.unauthorized();
      }
      return jwt;
    } catch (CommonException e) {
      throw e;
    } catch (Exception e) {
      throw CommonException.unauthorized();
    }
  }

  public static Verified verify(
      SignedJWT jwt, ECKey publicKey, String method, String target, long now) {
    try {
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      String jti = claims.getJWTID();
      if (!jwt.verify(new ECDSAVerifier(publicKey))
          || !method.equals(claims.getStringClaim("htm"))
          || !target.equals(claims.getStringClaim("htu"))
          || claims.getIssueTime() == null
          || jti == null
          || jti.isBlank()
          || jti.length() > MAX_JTI) {
        throw CommonException.unauthorized();
      }
      long iat = claims.getIssueTime().getTime();
      if (iat < now - WINDOW_MS || iat > now + WINDOW_MS) {
        throw CommonException.unauthorized();
      }
      return new Verified(jti, iat);
    } catch (CommonException e) {
      throw e;
    } catch (Exception e) {
      throw CommonException.unauthorized();
    }
  }

  /** A verified proof's replay key and issue time (epoch ms, whole seconds). */
  public record Verified(String jti, long iat) {}
}
