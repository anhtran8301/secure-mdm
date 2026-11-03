package com.anhtran.securemdm.security;

import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.exception.CommonException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Internal Device CA (ADR 0005): issues short-lived client certificates for device keys from a CSR.
 * The CA key is a separate key file; the CA certificate is rebuilt from it at startup.
 */
@Component
public class DeviceCertificateAuthority {
  private static final String SIGNATURE = "SHA256withECDSA";
  private static final X500Name CA_NAME = new X500Name("CN=Secure MDM Device CA,O=Secure MDM");
  private static final Date CA_NOT_BEFORE = new Date(1_767_225_600_000L); // 2026-01-01
  private static final Date CA_NOT_AFTER = new Date(2_524_608_000_000L); // 2050-01-01
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ECKey caKey;
  private final X509Certificate caCertificate;
  private final String caCertificatePem;

  @Autowired
  public DeviceCertificateAuthority(MdmProperties properties, SigningKeys keys) throws Exception {
    this(ECKey.parse(Files.readString(Path.of(properties.deviceCaKey()))), keys);
  }

  public DeviceCertificateAuthority(ECKey caKey, SigningKeys keys) throws Exception {
    Set<String> others = new HashSet<>();
    others.add(keys.getAccessKey().computeThumbprint().toString());
    keys.getCommandKeys().forEach(key -> others.add(thumbprint(key)));
    if (!caKey.isPrivate()
        || !Curve.P_256.equals(caKey.getCurve())
        || others.contains(caKey.computeThumbprint().toString())) {
      throw new IllegalArgumentException("Device CA needs its own private P-256 key");
    }
    this.caKey = caKey;
    var utils = new JcaX509ExtensionUtils();
    var builder =
        new JcaX509v3CertificateBuilder(
                CA_NAME,
                // RFC 5280 caps serials at 20 octets: use 16 bytes of the key thumbprint.
                new BigInteger(1, java.util.Arrays.copyOf(caKey.computeThumbprint().decode(), 16)),
                CA_NOT_BEFORE,
                CA_NOT_AFTER,
                CA_NAME,
                caKey.toECPublicKey())
            .addExtension(Extension.basicConstraints, true, new BasicConstraints(0))
            .addExtension(
                Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
            .addExtension(
                Extension.subjectKeyIdentifier,
                false,
                utils.createSubjectKeyIdentifier(caKey.toECPublicKey()));
    caCertificate = sign(builder);
    caCertificatePem = pem(caCertificate);
  }

  public String caCertificatePem() {
    return caCertificatePem;
  }

  /**
   * Verifies the CSR's self-signature (ECDSA with SHA-256 only) and key type, then issues a
   * certificate whose subject the server chooses ({@code CN=deviceId, O=organizationId}); the CSR
   * subject is ignored. Times are truncated to whole seconds, as X.509 stores them.
   */
  public IssuedCertificate issue(
      String csrPem, String organizationId, String deviceId, long notBefore, long notAfter) {
    ECPublicKey devicePublicKey = verifiedKey(csrPem);
    long from = notBefore / 1000 * 1000;
    long to = notAfter / 1000 * 1000;
    try {
      var utils = new JcaX509ExtensionUtils();
      BigInteger serial = new BigInteger(128, RANDOM).setBit(127);
      X500Name subject =
          new X500NameBuilder(BCStyle.INSTANCE)
              .addRDN(BCStyle.O, organizationId)
              .addRDN(BCStyle.CN, deviceId)
              .build();
      var builder =
          new JcaX509v3CertificateBuilder(
                  caCertificate, serial, new Date(from), new Date(to), subject, devicePublicKey)
              .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
              .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature))
              .addExtension(
                  Extension.extendedKeyUsage,
                  false,
                  new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth))
              .addExtension(
                  Extension.authorityKeyIdentifier,
                  false,
                  utils.createAuthorityKeyIdentifier(caCertificate))
              .addExtension(
                  Extension.subjectKeyIdentifier,
                  false,
                  utils.createSubjectKeyIdentifier(devicePublicKey));
      X509Certificate certificate = sign(builder);
      ECKey publicJwk = new ECKey.Builder(Curve.P_256, devicePublicKey).build();
      return new IssuedCertificate(
          serial.toString(16),
          pem(certificate),
          publicJwk.toJSONString(),
          HexFormat.of()
              .formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())),
          from,
          to);
    } catch (Exception e) {
      throw new IllegalStateException("Certificate issue failed", e);
    }
  }

  private static ECPublicKey verifiedKey(String csrPem) {
    try (var parser = new PEMParser(new StringReader(csrPem))) {
      if (!(parser.readObject() instanceof PKCS10CertificationRequest csr)
          || !X9ObjectIdentifiers.ecdsa_with_SHA256.equals(
              csr.getSignatureAlgorithm().getAlgorithm())
          || !csr.isSignatureValid(
              new JcaContentVerifierProviderBuilder().build(csr.getSubjectPublicKeyInfo()))
          || !(new JcaPKCS10CertificationRequest(csr).getPublicKey() instanceof ECPublicKey key)
          || !Curve.P_256.equals(Curve.forECParameterSpec(key.getParams()))) {
        throw CommonException.badRequest("Invalid CSR");
      }
      return key;
    } catch (CommonException e) {
      throw e;
    } catch (Exception e) {
      throw CommonException.badRequest("Invalid CSR");
    }
  }

  private X509Certificate sign(X509v3CertificateBuilder builder) throws Exception {
    ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE).build(caKey.toECPrivateKey());
    return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
  }

  private static String pem(X509Certificate certificate) throws Exception {
    var out = new StringWriter();
    try (var writer = new JcaPEMWriter(out)) {
      writer.writeObject(certificate);
    }
    return out.toString();
  }

  private static String thumbprint(ECKey key) {
    try {
      return key.computeThumbprint().toString();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * What the device module stores; {@code publicKeyJwk} is the only key proofs are checked with.
   */
  public record IssuedCertificate(
      String serial,
      String pem,
      String publicKeyJwk,
      String fingerprint,
      long notBefore,
      long notAfter) {}
}
