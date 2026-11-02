package com.anhtran.securemdm.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class GlobalExceptionHandlerTests {
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client =
        WebTestClient.bindToController(new ErrorController())
            .controllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void businessErrorPreservesCodeMessageAndStatus() {
    client
        .get()
        .uri("/business")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("MDM01")
        .jsonPath("$.message")
        .isEqualTo("Device not found")
        .jsonPath("$.status")
        .isEqualTo("NOT_FOUND");
  }

  @Test
  void reactiveBusinessErrorIsHandled() {
    client
        .get()
        .uri("/reactive")
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("MDM02")
        .jsonPath("$.status")
        .isEqualTo("CONFLICT");
  }

  @Test
  void beanValidationReturnsFieldMap() {
    client
        .post()
        .uri("/input")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"\"}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Name is required");
  }

  @Test
  void malformedJsonRemainsBadRequest() {
    client
        .post()
        .uri("/input")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{broken")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void unsupportedMediaTypeRemains415() {
    client
        .post()
        .uri("/input")
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue("text")
        .exchange()
        .expectStatus()
        .isEqualTo(415);
  }

  @Test
  void springErrorPreservesStatusAndHeaders() {
    client
        .get()
        .uri("/limited")
        .exchange()
        .expectStatus()
        .isEqualTo(429)
        .expectHeader()
        .valueEquals("Retry-After", "60");
  }

  @Test
  void unexpectedErrorHidesInternalMessage() {
    client
        .get()
        .uri("/unexpected")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .json(
            """
            {"code":"9999","message":"Internal server error","status":"INTERNAL_SERVER_ERROR"}
            """)
        .jsonPath("$.trace")
        .doesNotExist();
  }

  @Test
  void springServerErrorAlsoHidesInternalMessage() {
    client
        .get()
        .uri("/spring-server-error")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(503)
        .jsonPath("$.detail")
        .doesNotExist();
  }

  @RestController
  static class ErrorController {
    @GetMapping("/business")
    String business() {
      throw new CommonException("MDM01", "Device not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/reactive")
    Mono<String> reactive() {
      return Mono.error(
          new CommonException("MDM02", "Device already enrolled", HttpStatus.CONFLICT));
    }

    @PostMapping("/input")
    Mono<Input> input(@Valid @RequestBody Mono<Input> input) {
      return input;
    }

    @GetMapping("/limited")
    String limited() {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS) {
        @Override
        public HttpHeaders getHeaders() {
          var headers = new HttpHeaders();
          headers.set("Retry-After", "60");
          return headers;
        }
      };
    }

    @GetMapping("/unexpected")
    Mono<String> unexpected() {
      return Mono.error(new IllegalStateException("database password=secret"));
    }

    @GetMapping("/spring-server-error")
    String springServerError() {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "database password=secret");
    }
  }

  record Input(@NotBlank(message = "Name is required") String name) {}
}
