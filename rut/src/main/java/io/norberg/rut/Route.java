package io.norberg.rut;

import java.util.List;

import static io.norberg.rut.Objects.requireNonNull;

public final class Route {

  private final String method;
  private final Path path;

  private Route(final String method, final Path path) {
    this.method = requireNonNull(method, "method");
    this.path = requireNonNull(path, "path");
  }

  public String method() {
    return method;
  }

  public String pathString() {
    return path.toString();
  }

  public List<String> captureNames() {
    return path.captureNames();
  }

  Path path() {
    return path;
  }

  @Override
  public String toString() {
    return method + " " + path;
  }

  public static Route of(final CharSequence method, final CharSequence uri) {
    return new Route(method.toString(), Path.of(uri.toString()));
  }
}
