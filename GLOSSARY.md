# Secure MDM

Multi-tenant management of (simulated) Android devices: admins enroll devices, assign policies and
send signed commands; devices report state back.

## Tenancy and people

**Organization**:
The tenant. Every user, device, policy and command belongs to exactly one organization.
_Avoid_: Tenant (in UI text), company, workspace

**User**:
A person signed in to the admin console, with exactly one role in one organization.
_Avoid_: Account, member

**Role**:
What a user may do: ORG_ADMIN (enrollment, revocation), OPERATOR (policies, commands) or VIEWER (read only).

**Actor**:
Whoever performed an action, as recorded in the audit log: a user, a device, or the system.

**Session**:
A user's sign-in, lasting at most seven days, kept alive by rotating refresh tokens.

## Devices

**Device**:
An enrolled (simulated) Android device belonging to one organization.
_Avoid_: Agent, endpoint, client

**Enrollment Token**:
A one-time, short-lived secret an admin issues so that one device can enroll.
_Avoid_: Invite, registration code

**Device Key**:
The EC P-256 key pair a device generates at enrollment; the private half never leaves the device.
_Avoid_: Device secret

**CSR**:
The certificate signing request a device sends at enrollment, signed by its device key.

**Device CA**:
The backend's internal certificate authority that issues device certificates.

**Device Certificate**:
The X.509 certificate binding a device key to one device, valid 30 days; revoking the device revokes it.
_Avoid_: Device credential (the old bearer secret, now removed)

**Device Proof**:
A short signed token a device attaches to every request, proving it holds the device key.

**Heartbeat**:
A device's periodic check-in with its current inventory.

**Inventory**:
The device state it reports: screen lock, disk encryption, Android API level, installed packages.

**Connectivity**:
ONLINE when a heartbeat arrived within the offline window, otherwise OFFLINE; REVOKED once revoked.

**Revocation**:
Permanently disabling a device: its certificate is revoked and its open commands are closed.

## Policies

**Policy**:
A named set of compliance rules, evolved through versions.

**Policy Version**:
An immutable snapshot of a policy's rules. Changing a policy always creates a new version.
_Avoid_: Revision (that word belongs to assignments)

**Assignment**:
The single policy version a device must currently comply with.

**Assignment Revision**:
A per-device counter that increases on every assign or unassign, so stale reports can be rejected.

**Compliance**:
A device's standing against its assignment: PENDING, COMPLIANT, NON_COMPLIANT or UNKNOWN.

**Policy Report**:
A device's evaluation of its assignment, bound to the assignment revision it was made for.

## Commands

**Command**:
A signed instruction for one device (REPORT_STATUS, SYNC_DEVICE, SHOW_MESSAGE, LOCK_DEVICE_SIMULATED).
_Avoid_: Job, task, action

**Command Key**:
A key the backend signs commands with. One is active (signs); several may be published (trusted).

**Trust Bundle**:
The signed list of published command keys a device fetches to learn about key rotation.

**Claim**:
A device taking the oldest pending command from its queue.

**Attempt**:
One delivery of a command to a device, identified so that late results can be rejected.

**Lease**:
The time an attempt may run before the command returns to the queue or becomes UNKNOWN.

**UNKNOWN**:
A command that was delivered but whose outcome cannot be determined; distinct from FAILED.

**Idempotency Key**:
A client-chosen key that makes repeating the same command request return the original command.

**Audit Log**:
The append-only record of security-relevant actions, written in the same transaction as the change.
