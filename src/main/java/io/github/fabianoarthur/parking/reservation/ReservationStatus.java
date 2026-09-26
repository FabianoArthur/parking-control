package io.github.fabianoarthur.parking.reservation;

/**
 * An ACTIVE reservation whose window has passed simply stops being in effect; queries compare its
 * window with the current time, so no background job is needed to expire it.
 */
public enum ReservationStatus {
  ACTIVE,
  FULFILLED,
  CANCELLED
}
