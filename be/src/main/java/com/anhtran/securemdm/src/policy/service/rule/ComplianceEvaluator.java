package com.anhtran.securemdm.src.policy.service.rule;

import com.anhtran.securemdm.src.device.dto.model.Inventory;
import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import java.util.ArrayList;
import java.util.List;

/** Pure policy evaluation; an empty result means compliant. */
public final class ComplianceEvaluator {
  private ComplianceEvaluator() {}

  public static List<String> reasons(PolicyConfig policy, Inventory inventory) {
    List<String> reasons = new ArrayList<>();
    if (policy.requireScreenLock() && !inventory.screenLockEnabled()) {
      reasons.add("SCREEN_LOCK_REQUIRED");
    }
    if (policy.requireDiskEncryption() && !inventory.diskEncrypted()) {
      reasons.add("DISK_ENCRYPTION_REQUIRED");
    }
    if (inventory.androidApiLevel() < policy.minAndroidApiLevel()) {
      reasons.add("OS_VERSION_TOO_LOW");
    }
    if (inventory.installedPackages().stream().anyMatch(policy.blockedPackages()::contains)) {
      reasons.add("BLOCKED_PACKAGE_INSTALLED");
    }
    return reasons;
  }
}
