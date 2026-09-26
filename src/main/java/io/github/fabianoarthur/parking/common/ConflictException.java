package io.github.fabianoarthur.parking.common;

import org.springframework.http.HttpStatus;

/** The request clashes with the current state of a resource (HTTP 409). */
public class ConflictException extends DomainException {
  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
