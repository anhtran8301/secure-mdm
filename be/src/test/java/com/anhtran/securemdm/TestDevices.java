package com.anhtran.securemdm;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.StringWriter;
import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/** Test-only device: key pair, CSR and Device Proofs, as the simulator would make them. */
final class TestDevices {
  private TestDevices() {}

  static ECKey newKey() {
    try {
      return new ECKeyGenerator(Curve.P_256).generate();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String csr(KeyPair pair, String subject) {
    return csr(pair, subject, "SHA256withECDSA");
  }

  static String csr(KeyPair pair, String subject, String algorithm) {
    try {
      var request =
          new JcaPKCS10CertificationRequestBuilder(new X500Name(subject), pair.getPublic())
              .build(new JcaContentSignerBuilder(algorithm).build(pair.getPrivate()));
      var out = new StringWriter();
      try (var pem = new JcaPEMWriter(out)) {
        pem.writeObject(request);
      }
      return out.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String csr(ECKey key) {
    try {
      return csr(key.toKeyPair(), "CN=ignored-by-server");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String proof(ECKey key, String serial, String method, String target, long iatMillis) {
    return proof(key, serial, method, target, iatMillis, UUID.randomUUID().toString());
  }

  static String proof(
      ECKey key, String serial, String method, String target, long iatMillis, String jti) {
    try {
      var jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256)
                  .type(new JOSEObjectType("mdm-device-proof+jwt"))
                  .keyID(serial)
                  .build(),
              new JWTClaimsSet.Builder()
                  .claim("htm", method)
                  .claim("htu", target)
                  .issueTime(new Date(iatMillis))
                  .jwtID(jti)
                  .build());
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
