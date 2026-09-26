package io.github.fabianoarthur.parking.common;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {
  public NotFoundException(String resource, UUID id) {
    super(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " " + id + " was not found");
  }

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
  }
}
