package io.norberg.rut;

final class Objects {

  private Objects() {
    throw new AssertionError();
  }

  static <T> T requireNonNull(final T obj, final String message, final Object... args) {
    if (obj == null) {
      throw new NullPointerException(String.format(message, args));
    }
    return obj;
  }
}
