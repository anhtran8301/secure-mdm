# Spec: Device PKI

Device identity moves from a bearer secret to a key the device holds; command-signing keys can be
rotated without touching devices by hand; policies can require disk encryption. Decisions are in
[ADR 0005](../adr/0005-device-identity-x509-proof-of-possession.md) and
[ADR 0006](../adr/0006-command-key-rotation-signed-trust-bundle.md). Terms are in
[GLOSSARY.md](../../GLOSSARY.md).

## Acceptance criteria

### T1. Disk encryption

- Inventory carries `diskEncrypted`; a policy can set `requireDiskEncryption`.
- A device that reports `diskEncrypted=false` against such a policy is NON_COMPLIANT with reason
  `DISK_ENCRYPTION_REQUIRED`.
- Stored inventory and policy JSON written before this change still read (missing field = false).
- The simulator turns `diskEncrypted` on when its policy requires it; the console policy form has
  the new checkbox.

### T2. Command-key rotation

- The backend loads a set of command keys and signs with the one named by `mdm.command-active-kid`.
  Every key is P-256, kids are unique, no command key equals the access key.
- `GET /api/v1/device-agent/command-keys` (device only) returns `{bundles: [...]}`: one Trust
  Bundle signed by each published key, all with the same JWKS payload.
- The simulator replaces its trust store only with a bundle signed by a key it already trusts;
  otherwise it keeps the old store and carries on.
- Rotation is publish → switch → retire; retire waits at least the command TTL after switch.
- A device that missed the publish stage still catches up after the switch; a command signed before
  the switch still verifies after it.

### T3. Device identity

- Enrollment takes a CSR (PEM, EC P-256, self-signed by the device key) and returns the Device
  Certificate and the Device CA certificate. The server sets the subject (`CN=deviceId,
O=organizationId`), a random 128-bit serial, clientAuth EKU and a 30-day validity.
- A malformed CSR, a CSR not signed with ECDSA/SHA-256, a bad CSR signature or a non-P-256 key →
  400 with message `Invalid CSR`; the enrollment token stays unused.
- Device requests carry `Authorization: DeviceProof <jws>` (`typ=mdm-device-proof+jwt`, `kid` =
  certificate serial, claims `htm`, `htu`, `iat`, `jti`). It is accepted only if: the certificate
  exists, is not revoked, `notBefore ≤ now < notAfter`, its device is ACTIVE; the signature
  verifies with the stored public key; `htm`/`htu` match the request; `|now − iat| ≤ 60 s`; and
  `(device, jti)` has not been used before. Anything else → 401.
- A used `jti` is remembered until `iat + 60 s`, committed before the request's own work runs.
- Revoking a device revokes its certificates in the same transaction; its proofs then get 401.
- The bearer device credential no longer exists. Devices enrolled before the change are revoked by
  the migration (their open commands closed) and re-enroll with a new UID.

### T4. Certificate in the console

- `GET /devices/{id}` includes `certificate {serial, fingerprint, notAfter, revoked}`; the device
  page shows it.

## Tickets

| #   | Ticket                                                                        | Seams                                                            |
| --- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| T1  | Disk encryption in inventory and policy                                       | `LogicTests`, `WorkflowTests`, simulator Vitest, FE Vitest       |
| T2  | Command-key set, signed trust bundles, simulator trust refresh                | `LogicTests`, `WorkflowTests`, simulator Vitest, Java→TS fixture |
| T3  | Device CA, CSR enrollment, DeviceProof authentication, certificate revocation | `LogicTests`, `WorkflowTests`, simulator Vitest, E2E             |
| T4  | Device certificate on the device page                                         | `WorkflowTests`, FE build, E2E                                   |

## Out of scope

Certificate renewal (devices re-enroll), key attestation, mTLS, CRL/OCSP endpoints, hardware-backed
keys (the simulator's key is software-backed), KMS.
