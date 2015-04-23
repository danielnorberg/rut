package io.norberg.rut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.norberg.rut.Encoding.decode;
import static io.norberg.rut.Router.Status.METHOD_NOT_ALLOWED;
import static io.norberg.rut.Router.Status.NOT_FOUND;
import static io.norberg.rut.Router.Status.SUCCESS;

/**
 * A router for routing REST request paths to endpoints.
 *
 * @param <T> The target endpoint type.
 */
public final class Router<T> {

  private final RadixTrie<Route<T>> trie;
  private final boolean optionalTrailingSlash;

  private Router(final RadixTrie<Route<T>> trie, final boolean optionalTrailingSlash) {
    this.trie = trie;
    this.optionalTrailingSlash = optionalTrailingSlash;
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  @SuppressWarnings("UnusedParameters")
  public static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>();
  }

  /**
   * Route a request.
   *
   * @param method The request method. E.g. {@code GET, PUT, POST, DELETE}, etc.
   * @param path   The request path. E.g. {@code /foo/baz/bar}.
   * @param result A {@link Result} for storing the routing result, target and captured parameters.
   *               The {@link Result} should have enough capacity to store all captured parameters.
   *               See {@link #result()}.
   * @return Routing status. {@link Status#SUCCESS} if an endpoint and matching method was found.
   * {@link Status#NOT_FOUND} if the endpoint could not be found, {@link Status#METHOD_NOT_ALLOWED}
   * if the endpoint was found but the method did not match.
   */
  public Status route(final CharSequence method, final CharSequence path, final Result<T> result) {
    result.captor.optionalTrailingSlash(optionalTrailingSlash);
    final Route<T> route = trie.lookup(path, result.captor);
    if (route == null) {
      return result.notFound().status();
    }
    final Target<T> target = route.lookup(method);
    if (target == null) {
      return result.notAllowed(route).status();
    }
    return result.success(path, route, target).status();

  }

  /**
   * Create a {@link Result} with enough capacity to hold all captured parameters for any endpoint
   * of this router. The {@link Result} is intended to be instantiated reused, to avoid garbage.
   * Note that the {@link Result} is not thread safe, so a dedicated {@link Result} should be
   * created per thread.
   */
  public Result<T> result() {
    return Result.capturing(trie.captures());
  }

  /**
   * Routing result.
   */
  public enum Status {
    /**
     * A matching endpoint and method was found.
     */
    SUCCESS,

    /**
     * A matching endpoint was not found.
     */
    NOT_FOUND,

    /**
     * A matching endpoint was found but no method matched.
     */
    METHOD_NOT_ALLOWED
  }

  /**
   * Routing target holder.
   */
  private static class Target<T> {

    private final T target;
    private final String[] paramNames;

    private Target(final T target, final String[] paramNames) {

      this.target = target;
      this.paramNames = paramNames;
    }
  }

  /**
   * Router builder.
   */
  public static class Builder<T> {

    private boolean optionalTrailingSlash;

    private Builder() {
    }

    private final RadixTrie.Builder<Route<T>> trie = RadixTrie.builder();

    /**
     * Create a new {@link Router} that will route requests to all endpoints registered with {@link
     * #route}.
     */
    public Router<T> build() {
      return new Router<T>(trie.build(), optionalTrailingSlash);
    }

    /**
     * Register a routing path and method.
     *
     * @param method A method that should be accepted for the route.
     * @param path   The path of the route.
     * @param target A routing target that will be returned when requests are successfully routed to
     *               this route.
     */
    public Builder<T> route(final String method, final String path, final T target) {
      trie.insert(path, new RouteVisitor(method, target));
      return this;
    }

    /**
     * Set trailing slash matching to be optional or not. When configured to be optional, trailing
     * slash in both routed uris/paths and routes are disregarded. E.g., {@code /foo} may be routed
     * to {@code /foo/} and conversely {@code /bar/} may be routed to {@code /bar}.
     *
     * @param optional {@code true} if trailing slashes should be disregarded, {@code false} if
     *                 trailing slashes should be strictly routed.
     */
    public Builder<T> optionalTrailingSlash(final boolean optional) {
      this.optionalTrailingSlash = optional;
      return this;
    }

    /**
     * A {@link Trie.Visitor} that adds a {@link Route} to the terminal {@link Trie.Node}.
     */
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
        final Target<T> target = new Target<T>(this.target, paramNames);
        if (currentValue == null) {
          return Route.of(method, target);
        }
        return currentValue.with(method, target);
      }
    }
  }

  /**
   * Routing result holder.
   *
   * @param <T> The router endpoint type.
   */
  public static class Result<T> {

    private final RadixTrie.Captor captor;

    private Status status;
    private Route<T> route;
    private Target<T> target;
    private CharSequence path;

    private Result(final int captures) {
      captor = new RadixTrie.Captor(captures);
    }

    /**
     * Create a new {@link Result} with enough capacity to hold {@code captures} captured
     * parameters.
     */
    public static <T> Result<T> capturing(final int captures) {
      return new Result<T>(captures);
    }

    /**
     * Get routing status of the {@link Router#route} invocation.
     */
    public Status status() {
      return status;
    }

    /**
     * Convenience method for checking if the routing status is {@link Status#SUCCESS}.
     */
    public boolean isSuccess() {
      return status() == SUCCESS;
    }

    /**
     * Get routing target, if successful.
     */
    public T target() {
      if (target == null) {
        throw new IllegalStateException("not matched");
      }
      return target.target;
    }

    /**
     * Get the number of captured parameter values.
     */
    public int params() {
      return captor.values();
    }

    /**
     * Get the name of the captured path parameter at index {code i}.
     */
    public String paramName(final int i) {
      if (target == null) {
        throw new IllegalStateException("not matched");
      }
      return target.paramNames[i];
    }

    /**
     * Get the value of the captured path parameter at index {code i}.
     */
    public CharSequence paramValue(final int i) {
      return captor.value(path, i);
    }

    /**
     * Get the URL decoded value of the captured path parameter at index {code i}.
     *
     * @return The decoded value or null if the encoding is invalid.
     */
    public CharSequence paramValueDecoded(final int i) {
      return decode(paramValue(i));
    }

    /**
     * Get start offset into the routed path of the captured parameter at index {code i}.
     *
     * @see #paramValue
     * @see #paramValueEnd
     */
    public int paramValueStart(final int i) {
      return captor.valueStart(i);
    }

    /**
     * Get end offset into the routed path of the captured parameter at index {code i}.
     *
     * @see #paramValue
     * @see #paramValueStart
     */
    public int paramValueEnd(final int i) {
      return captor.valueEnd(i);
    }

    /**
     * Signal a route found but method not allowed.
     */
    private Result<T> notAllowed(final Route<T> route) {
      this.status = METHOD_NOT_ALLOWED;
      this.route = route;
      this.target = null;
      this.path = null;
      return this;
    }

    /**
     * Signal no route found.
     */
    private Result<T> notFound() {
      this.status = NOT_FOUND;
      this.route = null;
      this.target = null;
      this.path = null;
      return this;
    }

    /**
     * Signal a routing success.
     */
    private Result<T> success(final CharSequence path, final Route<T> route,
                              final Target<T> target) {
      this.status = SUCCESS;
      this.route = route;
      this.target = target;
      this.path = path;
      return this;
    }

    /**
     * Get query string start index. -1 if there is no query string part.
     */
    public int queryStart() {
      return captor.queryStart();
    }

    /**
     * Get query string end index. -1 if there is no query string part.
     */
    public int queryEnd() {
      return captor.queryEnd();
    }

    /**
     * Get query string. null if there is no query string part.
     */
    public CharSequence query() {
      return captor.query(path);
    }

    /**
     * Get all allowed methods for the route if {@link #status()} is {@link Status#SUCCESS} or
     * {@link Status#METHOD_NOT_ALLOWED}. Returns an empty collection if {@link #status()} is {@link
     * Status#NOT_FOUND}.
     */
    public Collection<String> allowedMethods() {
      if (route == null) {
        throw new IllegalStateException("not matched");
      }
      return route.methods();
    }
  }

  /**
   * Holder for route methods and target endpoints.
   */
  private static class Route<T> {

    private final String method;
    private final Target<T> target;
    private final Route<T> next;
    private final Collection<String> methods;

    private Route(final String method, final Target<T> target, final Route<T> next) {
      this.method = method;
      this.target = target;
      this.next = next;
      this.methods = methods0();
    }

    /**
     * Create a new route.
     */
    private static <T> Route<T> of(final String method, final Target<T> target) {
      return new Route<T>(method, target, null);
    }

    /**
     * Add a new method and target to this route.
     */
    private Route<T> with(final String method, final Target<T> target) {
      return new Route<T>(method, target, this);
    }

    /**
     * Look up a method in this route.
     *
     * @return The endpoint if the method matched. {@code null} otherwise.
     */
    private Target<T> lookup(final CharSequence method) {
      Route<T> route = this;
      while (route != null) {
        if (equals(route.method, method)) {
          return route.target;
        }
        route = route.next;
      }
      return null;
    }

    /**
     * Compare two {@link CharSequence}s.
     */
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

    /**
     * Get a {@link Collection} of {@link String} with all methods allowed by this endpoint.
     */
    public Collection<String> methods() {
      return methods;
    }

    /**
     * Create a list of all methods allowed by this endpoint.
     */
    private Collection<String> methods0() {
      final List<String> methods = new ArrayList<String>();
      Route<T> route = this;
      while (route != null) {
        methods.add(route.method);
        route = route.next;
      }
      return Collections.unmodifiableList(methods);
    }
  }
}
