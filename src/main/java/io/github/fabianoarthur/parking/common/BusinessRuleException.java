package io.github.fabianoarthur.parking.common;

import org.springframework.http.HttpStatus;

/** The request is well formed but breaks a business rule (HTTP 422). */
public class BusinessRuleException extends DomainException {
  public BusinessRuleException(String code, String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
  }
}
