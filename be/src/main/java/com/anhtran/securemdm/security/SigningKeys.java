package com.anhtran.securemdm.security;

import com.anhtran.securemdm.config.MdmProperties;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the ES256 private keys: one for admin access tokens and a set of command keys, of which one
 * is active (signs) and all are published (devices trust them; see ADR 0006).
 */
@Getter
@Component
public class SigningKeys {
  private final ECKey accessKey;
  private final ECKey commandKey;
  private final List<ECKey> commandKeys;

  @Autowired
  public SigningKeys(MdmProperties properties) throws Exception {
    this(properties.accessKey(), properties.commandKeys(), properties.commandActiveKid());
  }

  public SigningKeys(String accessFile, String commandFile) throws Exception {
    this(accessFile, List.of(commandFile), null);
  }

  public SigningKeys(String accessFile, List<String> commandFiles, String activeKid)
      throws Exception {
    accessKey = read(accessFile);
    List<ECKey> loaded = new ArrayList<>();
    Set<String> kids = new HashSet<>();
    Set<String> thumbprints = new HashSet<>(Set.of(accessKey.computeThumbprint().toString()));
    for (String file : commandFiles) {
      ECKey key = read(file);
      if (!kids.add(key.getKeyID()) || !thumbprints.add(key.computeThumbprint().toString())) {
        throw new IllegalArgumentException("Command keys need unique kids and separate keys");
      }
      loaded.add(key);
    }
    if (loaded.isEmpty()) {
      throw new IllegalArgumentException("At least one command key required");
    }
    commandKeys = List.copyOf(loaded);
    if (activeKid == null || activeKid.isBlank()) {
      if (loaded.size() != 1) {
        throw new IllegalArgumentException("Name the active command key among several");
      }
      commandKey = loaded.get(0);
    } else {
      commandKey =
          loaded.stream()
              .filter(key -> key.getKeyID().equals(activeKid))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Active command key not loaded"));
    }
  }

  private static ECKey read(String file) throws Exception {
    ECKey key = ECKey.parse(Files.readString(Path.of(file)));
    if (!key.isPrivate()
        || !Curve.P_256.equals(key.getCurve())
        || key.getKeyID() == null
        || key.getKeyID().isBlank()) {
      throw new IllegalArgumentException("Private P-256 key with a kid required: " + file);
    }
    return key;
  }
}
