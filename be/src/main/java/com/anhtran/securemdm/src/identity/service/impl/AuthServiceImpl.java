package com.anhtran.securemdm.src.identity.service.impl;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.ActorKind;
import com.anhtran.securemdm.security.DeviceProofRequest;
import com.anhtran.securemdm.security.DeviceProofs;
import com.anhtran.securemdm.security.JwtTokenProvider;
import com.anhtran.securemdm.security.Role;
import com.anhtran.securemdm.security.SecureTokens;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.src.device.repository.DeviceCertificateRepository;
import com.anhtran.securemdm.src.device.repository.DeviceProofReplayRepository;
import com.anhtran.securemdm.src.identity.dto.model.AuthTokens;
import com.anhtran.securemdm.src.identity.dto.request.LoginRequest;
import com.anhtran.securemdm.src.identity.dto.response.UserProfileResponse;
import com.anhtran.securemdm.src.identity.entity.AuthSession;
import com.anhtran.securemdm.src.identity.entity.RefreshToken;
import com.anhtran.securemdm.src.identity.entity.User;
import com.anhtran.securemdm.src.identity.repository.AuthSessionRepository;
import com.anhtran.securemdm.src.identity.repository.OrganizationRepository;
import com.anhtran.securemdm.src.identity.repository.RefreshTokenRepository;
import com.anhtran.securemdm.src.identity.repository.UserRepository;
import com.anhtran.securemdm.src.identity.repository.projection.RefreshSessionView;
import com.anhtran.securemdm.src.identity.service.AuthService;
import com.anhtran.securemdm.utils.AppConstants;
import com.anhtran.securemdm.utils.IdUtils;
import com.nimbusds.jose.jwk.ECKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private static final String SESSION = "session";

  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final AuthSessionRepository sessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final DeviceCertificateRepository certificateRepository;
  private final DeviceProofReplayRepository proofReplayRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final AuditService auditService;
  private final TransactionalOperator transactions;
  private final Clock clock;

  @Override
  public Mono<AuthTokens> login(LoginRequest request) {
    return userRepository
        .findActiveByOrganizationSlugAndEmail(
            request.organizationSlug(), request.email().toLowerCase(Locale.ROOT))
        .switchIfEmpty(Mono.error(CommonException.unauthorized()))
        .flatMap(
            user ->
                // BCrypt is CPU-bound; keep it off the event loop.
                Mono.fromCallable(
                        () -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(
                        valid ->
                            valid
                                ? startSession(user)
                                : Mono.error(CommonException.unauthorized())));
  }

  private Mono<AuthTokens> startSession(User user) {
    String sessionId = IdUtils.newId();
    long expiresAt = clock.millis() + AppConstants.SESSION_TTL_MS;
    Actor actor = toActor(user, sessionId);
    return sessionRepository
        .save(
            AuthSession.builder()
                .id(sessionId)
                .organizationId(actor.organizationId())
                .userId(actor.id())
                .expiresAt(expiresAt)
                .newEntity(true)
                .build())
        .then(issue(actor, expiresAt))
        .flatMap(
            tokens -> auditService.record(actor, "LOGIN", SESSION, sessionId).thenReturn(tokens))
        .as(transactions::transactional);
  }

  private Mono<AuthTokens> issue(Actor actor, long expiresAt) {
    String refreshToken = SecureTokens.generate(AppConstants.REFRESH_TOKEN_PREFIX);
    return refreshTokenRepository
        .save(
            RefreshToken.builder()
                .tokenHash(SecureTokens.hash(refreshToken))
                .sessionId(actor.sessionId())
                .newEntity(true)
                .build())
        .then(profile(actor))
        .map(user -> new AuthTokens(jwtTokenProvider.issue(actor), refreshToken, expiresAt, user));
  }

  @Override
  public Mono<UserProfileResponse> profile(Actor actor) {
    return organizationRepository
        .findById(actor.organizationId())
        .map(
            org ->
                UserProfileResponse.builder()
                    .id(actor.id())
                    .organizationId(actor.organizationId())
                    .organizationSlug(org.getSlug())
                    .role(actor.role())
                    .build());
  }

  @Override
  public Mono<AuthTokens> refresh(String refreshToken) {
    if (refreshToken == null) {
      return Mono.error(CommonException.unauthorized());
    }
    String hash = SecureTokens.hash(refreshToken);
    // Commit reuse revocation before returning 401; erroring inside the transaction would undo it.
    return refreshTokenRepository
        .lockForRefresh(hash)
        .flatMap(session -> rotate(session, hash))
        .as(transactions::transactional)
        .switchIfEmpty(Mono.error(CommonException.unauthorized()));
  }

  private Mono<AuthTokens> rotate(RefreshSessionView session, String hash) {
    String sessionId = session.getId();
    if (session.getRevokedAt() != null || session.getExpiresAt() <= clock.millis()) {
      return Mono.empty();
    }
    if (session.getUsedAt() != null) {
      Actor owner =
          new Actor(
              session.getOrganizationId(),
              session.getUserId(),
              ActorKind.USER.name(),
              ActorKind.USER.name(),
              sessionId);
      return sessionRepository
          .revoke(sessionId, clock.millis())
          .then(auditService.record(owner, "REFRESH_REUSE_REVOKED", SESSION, sessionId))
          .then(Mono.empty());
    }
    return userRepository
        .findActive(session.getOrganizationId(), session.getUserId())
        .flatMap(
            user ->
                refreshTokenRepository
                    .markUsed(hash, clock.millis())
                    .then(
                        auditService.record(
                            toActor(user, sessionId), "SESSION_REFRESHED", SESSION, sessionId))
                    .then(issue(toActor(user, sessionId), session.getExpiresAt())));
  }

  @Override
  public Mono<Void> logout(String refreshToken) {
    if (refreshToken == null) {
      return Mono.empty();
    }
    return refreshTokenRepository
        .lockForLogout(SecureTokens.hash(refreshToken))
        .flatMap(
            session ->
                sessionRepository
                    .revoke(session.getId(), clock.millis())
                    .then(
                        auditService.record(
                            new Actor(
                                session.getOrganizationId(),
                                session.getUserId(),
                                session.getRole(),
                                ActorKind.USER.name(),
                                session.getId()),
                            "LOGOUT",
                            SESSION,
                            session.getId())))
        .as(transactions::transactional);
  }

  @Override
  public Mono<Actor> authenticateDevice(DeviceProofRequest request) {
    return Mono.defer(
            () -> {
              var jwt = DeviceProofs.parse(request.proof());
              long now = clock.millis();
              return certificateRepository
                  .findUsable(jwt.getHeader().getKeyID(), now)
                  .flatMap(
                      cert -> {
                        ECKey key;
                        try {
                          key = ECKey.parse(cert.getPublicKeyJwk());
                        } catch (ParseException ex) {
                          return Mono.error(CommonException.unauthorized());
                        }
                        var proof =
                            DeviceProofs.verify(jwt, key, request.method(), request.target(), now);
                        // Own statement, committed before the request's work: a used jti stays
                        // used even if that work fails. Kept until the proof cannot pass anyway.
                        return proofReplayRepository
                            .remember(
                                cert.getDeviceId(),
                                proof.jti(),
                                proof.iat() + DeviceProofs.WINDOW_MS)
                            .onErrorMap(
                                DataIntegrityViolationException.class,
                                ex -> CommonException.unauthorized())
                            .thenReturn(
                                new Actor(
                                    cert.getOrganizationId(),
                                    cert.getDeviceId(),
                                    Role.DEVICE.name(),
                                    ActorKind.DEVICE.name(),
                                    null));
                      });
            })
        .switchIfEmpty(Mono.error(CommonException.unauthorized()));
  }

  @Override
  public Mono<Actor> authenticate(String bearerToken) {
    return Mono.defer(
            () -> {
              var claims = jwtTokenProvider.verify(bearerToken);
              String sessionId;
              String tenant;
              try {
                sessionId = claims.getStringClaim("sid");
                tenant = claims.getStringClaim("tenant");
              } catch (ParseException ex) {
                return Mono.error(CommonException.unauthorized());
              }
              return userRepository
                  .findBySession(sessionId, claims.getSubject(), tenant, clock.millis())
                  .map(user -> toActor(user, sessionId));
            })
        .switchIfEmpty(Mono.error(CommonException.unauthorized()));
  }

  private static Actor toActor(User user, String sessionId) {
    return new Actor(
        user.getOrganizationId(), user.getId(), user.getRole(), ActorKind.USER.name(), sessionId);
  }
}
