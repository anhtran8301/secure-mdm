package com.anhtran.securemdm.src.command.service.rule;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.src.command.entity.enums.CommandStatus;
import com.anhtran.securemdm.src.command.entity.enums.CommandType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure command rules: allowed payloads, lifecycle transitions and accepted results. */
public final class CommandStateMachine {
  public static final Set<String> TYPES =
      Arrays.stream(CommandType.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private static final String PENDING = CommandStatus.PENDING.name();
  private static final String IN_PROGRESS = CommandStatus.IN_PROGRESS.name();

  private CommandStateMachine() {}

  public static void validate(String type, Map<String, Object> payload) {
    boolean valid = TYPES.contains(type) && payload != null;
    if (valid && CommandType.SHOW_MESSAGE.name().equals(type)) {
      valid =
          payload.keySet().equals(Set.of("message"))
              && payload.get("message") instanceof String s
              && !s.isBlank()
              && s.length() <= 500;
    } else if (valid) {
      valid = payload.isEmpty();
    }
    if (!valid) {
      throw CommonException.badRequest("Invalid command type or payload");
    }
  }

  /**
   * Next status after time passes. Expiry before delivery is EXPIRED; once delivered, an unfinished
   * command is UNKNOWN. An expired lease retries until {@code max} attempts.
   */
  public static String transition(
      String status, int attempts, long now, long expires, Long lease, int max) {
    if (!PENDING.equals(status) && !IN_PROGRESS.equals(status)) {
      return status;
    }
    if (now >= expires) {
      return attempts == 0 ? CommandStatus.EXPIRED.name() : CommandStatus.UNKNOWN.name();
    }
    if (IN_PROGRESS.equals(status) && lease != null && now >= lease) {
      return attempts >= max ? CommandStatus.UNKNOWN.name() : PENDING;
    }
    return status;
  }

  public static void validateResult(String type, String status, Map<String, Object> result) {
    if (CommandStatus.FAILED.name().equals(status)) {
      if (result.keySet().equals(Set.of("errorCode"))
          && result.get("errorCode") instanceof String s
          && s.matches("[A-Z_]{1,60}")) {
        return;
      }
    } else if (CommandStatus.SUCCEEDED.name().equals(status)) {
      String flag =
          switch (type) {
            case "SYNC_DEVICE" -> "synced";
            case "SHOW_MESSAGE" -> "displayed";
            case "LOCK_DEVICE_SIMULATED" -> "locked";
            default -> null;
          };
      if (flag != null && result.equals(Map.of(flag, true))) {
        return;
      }
      if (CommandType.REPORT_STATUS.name().equals(type) && validInventoryResult(result)) {
        return;
      }
    }
    throw CommonException.badRequest("Invalid command result");
  }

  private static boolean validInventoryResult(Map<String, Object> result) {
    return result.keySet().equals(Set.of("inventory"))
        && result.get("inventory") instanceof Map<?, ?> inventory
        && inventory
            .keySet()
            .equals(
                Set.of(
                    "screenLockEnabled", "diskEncrypted", "androidApiLevel", "installedPackages"))
        && inventory.get("screenLockEnabled") instanceof Boolean
        && inventory.get("diskEncrypted") instanceof Boolean
        && inventory.get("androidApiLevel") instanceof Number n
        && n.doubleValue() == n.intValue()
        && n.intValue() >= 1
        && n.intValue() <= 100
        && inventory.get("installedPackages") instanceof List<?> packages
        && packages.size() <= 200
        && packages.stream()
            .allMatch(p -> p instanceof String s && !s.isBlank() && s.length() <= 180);
  }
}
