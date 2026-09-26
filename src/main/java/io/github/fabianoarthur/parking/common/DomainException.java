package io.github.fabianoarthur.parking.common;

import org.springframework.http.HttpStatus;

/** Base for errors the API reports as an RFC 7807 problem with a stable {@code code}. */
public abstract class DomainException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  protected DomainException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
