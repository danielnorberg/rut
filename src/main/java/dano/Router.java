package dano;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dano.Router.Status.METHOD_NOT_ALLOWED;
import static dano.Router.Status.NOT_FOUND;
import static dano.Router.Status.SUCCESS;

public final class Router<T> {

  private final RadixTrie<Route<T>> trie;

  private Router(final RadixTrie<Route<T>> trie) {
    this.trie = trie;
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  public static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>();
  }

  public Status route(final CharSequence method, final CharSequence path, final Result<T> result) {
    final Route<T> route = trie.lookup(path, result.captor);
    if (route == null) {
      result.failure(NOT_FOUND);
      return NOT_FOUND;
    }
    final Target<T> target = route.lookup(method);
    if (target == null) {
      result.failure(METHOD_NOT_ALLOWED);
      return METHOD_NOT_ALLOWED;
    }
    result.success(path, target);
    return SUCCESS;

  }

  public Result<String> result() {
    return new Result<String>(trie.captures());
  }

  public enum Status {METHOD_NOT_ALLOWED, NOT_FOUND, SUCCESS}

  public static class Target<T> {

    private final T target;
    private final String[] paramNames;

    public Target(final T target, final String[] paramNames) {

      this.target = target;
      this.paramNames = paramNames;
    }
  }

  public static class Builder<T> {

    private Builder() {
    }

    private final RadixTrie.Builder<Route<T>> trie = RadixTrie.builder();

    public Router<T> build() {
      return new Router<T>(trie.build());
    }

    public Builder<T> route(final String method, final String path, final T target) {
      trie.insert(path, new RouteVisitor(method, target));
      return this;
    }

    private class RouteVisitor implements Trie.Visitor<Route<T>> {

      private String[] paramNames;

      private final String method;
      private final T target;

      public RouteVisitor(final String method, final T target) {
        this.method = method;
        this.target = target;
      }

      @Override
      public void capture(final int i, final CharSequence s) {
        paramNames[i] = s.toString();
      }

      @Override
      public Route<T> finish(final int captures, final Route<T> currentValue) {
        paramNames = new String[captures];
        final Route<T> route = currentValue == null ? new Route<T>() : currentValue;
        route.method(method, new Target<T>(target, paramNames));
        return route;
      }
    }
  }

  public static class Result<T> {

    private final Captor captor;

    private Status status;
    private Target<T> target;
    private CharSequence path;

    public Result(final int captures) {
      captor = new Captor(captures);
    }

    public Status status() {
      return status;
    }

    public boolean isSuccess() {
      return status() == SUCCESS;
    }

    public T target() {
      return target.target;
    }

    public String paramName(final int i) {
      return target.paramNames[i];
    }

    public CharSequence paramValue(final int i) {
      return captor.value(path, i);
    }

    public int paramValueStart(final int i) {
      return captor.valueStart(i);
    }

    public int paramValueEnd(final int i) {
      return captor.valueEnd(i);
    }

    private void failure(final Status status) {
      this.status = status;
    }

    public void success(final CharSequence path, final Target<T> target) {
      this.path = path;
      this.target = target;
      this.status = SUCCESS;
    }
  }

  private static class Route<T> {

    private final List<Map.Entry<String, Target<T>>> methods =
        new ArrayList<Map.Entry<String, Target<T>>>();

    public Target<T> lookup(final CharSequence method) {
      for (final Map.Entry<String, Target<T>> entry : methods) {
        if (equals(entry.getKey(), method)) {
          return entry.getValue();
        }
      }
      return null;
    }

    private boolean equals(final CharSequence a, final CharSequence b) {
      if (a == b) {
        return true;
      }
      final int length = a.length();
      if (length != b.length()) {
        return false;
      }
      for (int i = 0; i < length; i++) {
        if (a.charAt(i) != b.charAt(i)) {
          return false;
        }
      }
      return true;
    }

    public void method(final String method, final Target<T> target) {
      methods.add(new AbstractMap.SimpleEntry<String, Target<T>>(method, target));
    }
  }
}
