package com.anhtran.securemdm.security;

/** A Device Proof with the method and target (path and query) of the request it came with. */
public record DeviceProofRequest(String proof, String method, String target) {}
