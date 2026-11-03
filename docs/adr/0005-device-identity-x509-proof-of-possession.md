# Device identity: X.509 certificate from a CSR, requests signed with the device key

A bearer device credential works for whoever copies it. Devices now generate their own EC P-256
key, send a CSR at enrollment and receive a Device Certificate issued by an internal Device CA
(BouncyCastle; the CA key is a separate key file, never equal to the access or command keys). The
server ignores the CSR subject and sets its own (`CN=deviceId, O=organizationId`).

Every device request carries a Device Proof: a short ES256 JWS over method, path, issue time and a
random `jti`, with the certificate serial as `kid`. The server verifies it with the public key it
stored at issue time, never with key material sent in the request. A proof is accepted within
±60 s of `iat`, and each `jti` is recorded until `iat + 60 s`, committed before the request's own
work, so a proof cannot be replayed even if that work fails.

Considered:

- **mTLS** is the usual answer, but it moves trust to a TLS terminator and a forwarded header,
  which is easy to misconfigure and hard to test end to end in this demo. The proof gives the same
  proof-of-possession inside the app and is covered by `WorkflowTests`.
- **Signing the body** was left out, as in DPoP (RFC 9449): TLS protects the body in transit and
  the proof binds the request to the key, method, path and time.
- **Keeping the bearer credential alongside** was rejected: two auth paths double the tests and the
  attack surface. Devices enrolled before this change must enroll again.

There is no renewal yet: after 30 days a device enrolls again with a new UID (UIDs stay unique per
organization, even for revoked devices). A real agent would renew with a proof-signed CSR before
expiry.

Revocation stays a database flag checked on every request (no CRL/OCSP), which is enough while the
server is the only relying party. The simulator's key is software-backed; a real agent would keep
it in Android Keystore/StrongBox or the Secure Enclave.
