package io.norberg.rut;

/**
 * Path parameter type.
 */
public enum ParameterType {
  /**
   * A path parameter that cannot span segments.
   */
  SEGMENT,
  /**
   * A path parameter that can include several segments.
   */
  PATH
}
