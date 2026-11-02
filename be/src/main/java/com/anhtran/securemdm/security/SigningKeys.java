package com.anhtran.securemdm.security;

import com.anhtran.securemdm.config.MdmProperties;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Loads the separate ES256 private keys for admin access tokens and device commands. */
@Getter
@Component
public class SigningKeys {
  private final ECKey accessKey;
  private final ECKey commandKey;

  @Autowired
  public SigningKeys(MdmProperties properties) throws Exception {
    this(properties.accessKey(), properties.commandKey());
  }

  public SigningKeys(String accessFile, String commandFile) throws Exception {
    accessKey = ECKey.parse(Files.readString(Path.of(accessFile)));
    commandKey = ECKey.parse(Files.readString(Path.of(commandFile)));
    if (!valid(accessKey)
        || !valid(commandKey)
        || accessKey.computeThumbprint().equals(commandKey.computeThumbprint())) {
      throw new IllegalArgumentException("Separate private access and command keys required");
    }
  }

  private static boolean valid(ECKey key) {
    return key.isPrivate()
        && Curve.P_256.equals(key.getCurve())
        && key.getKeyID() != null
        && !key.getKeyID().isBlank();
  }
}
