# Command-key rotation through signed trust bundles

Devices verify commands against a provisioned JWKS, and rotating the command key used to mean
editing that file on every device. The backend now holds a set of command keys and signs with one
active key. Devices fetch `GET /device-agent/command-keys`, which returns the same JWKS payload
signed once by every published key; a device accepts a bundle only if it is signed by a key it
already trusts, so trust can only be extended by a key that was trusted before. If no bundle
verifies, the device keeps its old trust and carries on.

Rotation has three stages so that stopping signing and stopping trusting are separate:

1. **Publish**: active = old, published = {old, new}. Devices learn the new key.
2. **Switch**: active = new, published = {old, new}. Old commands still verify, and devices that
   missed stage 1 still catch up through the bundle signed by the old key.
3. **Retire**: published = {new}, no earlier than the command TTL after the switch, when every
   command signed by the old key has expired.

A device that did not sync at all between publish and retire must be provisioned with trust again.
Considered: trusting whatever JWKS the server returns over TLS (trust on every use, so a
compromised server or proxy could swap keys), and a separate long-lived root key that signs
bundles (one more key to protect and rotate, with no gain in this demo).
