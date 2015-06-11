package io.norberg.rut;

import java.util.ArrayList;
import java.util.List;

import static io.norberg.rut.CharSequences.indexOf;
import static io.norberg.rut.Objects.requireNonNull;
import static io.norberg.rut.ParameterType.PATH;
import static io.norberg.rut.ParameterType.SEGMENT;
import static java.util.Collections.unmodifiableList;

final class Path {

  private final List<Part> parts;
  private final List<String> captureNames;
  private final List<ParameterType> captureParameterTypes;
  private final String string;

  Path(final List<Part> parts) {
    this.parts = requireNonNull(parts, "parts");
    if (parts.isEmpty()) {
      throw new IllegalArgumentException();
    }
    final List<String> captureNames = new ArrayList<String>();
    final List<ParameterType> parameterTypes = new ArrayList<ParameterType>();
    for (final Part part : parts) {
      if (part instanceof Capture) {
        captureNames.add(((Capture) part).name());

        if (part instanceof CaptureSegment) {
          parameterTypes.add(SEGMENT);
        }
        else {
          parameterTypes.add(PATH);
        }
      }
    }
    this.captureNames = unmodifiableList(captureNames);
    this.captureParameterTypes = unmodifiableList(parameterTypes);
    this.string = join(parts);
  }

  List<Part> parts() {
    return parts;
  }

  List<String> captureNames() {
    return captureNames;
  }

  @Override
  public String toString() {
    return string;
  }

  static Path of(final CharSequence path) {
    return of(path.toString());
  }

  static Path of(final String path) {
    return new Path(parts(path));
  }

  public List<ParameterType> captureParameterTypes() {
    return captureParameterTypes;
  }

  interface Part {

  }

  interface Capture extends Part {

    String name();
  }

  static final class Match implements Part {

    private final String string;

    Match(final String string) {
      this.string = string;
    }

    String string() {
      return string;
    }

    @Override
    public String toString() {
      return string;
    }
  }

  static final class CaptureSegment implements Capture {

    private final String name;

    CaptureSegment(final String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String toString() {
      return "<" + name + ">";
    }
  }

  static final class CapturePath implements Capture {

    private final String name;

    CapturePath(final String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String toString() {
      return "<" + name + ":path>";
    }
  }

  private enum State {
    PREFIX,
    CAPTURE
  }

  private static List<Part> parts(final String path) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException("Empty path");
    }
    final List<Part> parts = new ArrayList<Part>();
    State state = State.PREFIX;
    int start = 0;
    for (int i = 0; i < path.length(); i++) {
      final char c = path.charAt(i);
      if (c > 127) {
        throw new IllegalArgumentException();
      }
      switch (state) {
        case PREFIX:
          if (c == '<') {
            if (i != start) {
              parts.add(new Match(path.substring(start, i)));
            }
            start = i + 1;
            state = State.CAPTURE;
          }
          break;
        case CAPTURE:
          if (c == '>') {
            final Capture capture = capture(path, start, i);
            if (capture instanceof CapturePath && path.length() > i + 1) {
              throw new IllegalArgumentException("path capture must be last");
            }
            parts.add(capture);
            start = i + 1;
            state = State.PREFIX;
          }
          break;
      }
    }
    if (state == State.CAPTURE) {
      throw new IllegalArgumentException("unclosed capture: " + path);
    }
    if (start < path.length()) {
      parts.add(new Match(path.substring(start)));
    }
    return parts;
  }

  private static Capture capture(final String path, final int start, final int end) {
    final int colon = indexOf(path, ':', start + 1, end);
    if (colon == -1) {
      return new CaptureSegment(path.substring(start, end));
    }
    final String name = path.subSequence(start, colon).toString();
    final String type = path.subSequence(colon + 1, end).toString();
    if ("path".equals(type)) {
      return new CapturePath(name);
    }
    throw new IllegalArgumentException("Unknown capture type: " + name);
  }

  private static String join(final List<Part> parts) {
    final StringBuilder b = new StringBuilder();
    for (final Part part : parts) {
      b.append(part.toString());
    }
    return b.toString();
  }
}
