package io.norberg.rut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

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

  private final RadixTrie<RouteTarget<T>> trie;
  private final boolean optionalTrailingSlash;

  private Router(final RadixTrie<RouteTarget<T>> trie, final boolean optionalTrailingSlash) {
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
    final RouteTarget<T> route = trie.lookup(path, result.captor);
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
    private final ParameterType[] paramTypes;

    private Target(final T target, final String[] paramNames, ParameterType[] paramTypes) {
      this.target = target;
      this.paramNames = paramNames;
      this.paramTypes = paramTypes;
    }
  }

  /**
   * Router builder.
   */
  public static class Builder<T> {

    private boolean optionalTrailingSlash;

    private Builder() {
    }

    private final RadixTrie.Builder<RouteTarget<T>> trie = RadixTrie.builder();

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
      return route(Route.of(method, path), target);
    }

    /**
     * Register a route.
     *
     * @param route  The route to register.
     * @param target A routing target that will be returned when requests are successfully routed
     *               to this route.
     */
    public Builder<T> route(final Route route, final T target) {
      trie.insert(route.path(), new RouteVisitor(route, target));
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
     * A {@link Trie.Visitor} that adds a {@link RouteTarget} to the terminal {@link Trie.Node}.
     */
    private class RouteVisitor implements Trie.Visitor<RouteTarget<T>> {


      private final Route route;
      private final T target;

      public RouteVisitor(final Route route, final T target) {
        this.route = route;
        this.target = target;
      }

      @Override
      public RouteTarget<T> finish(final RouteTarget<T> currentValue) {
        final List<String> captureNames = route.captureNames();
        final String[] paramNames = captureNames.toArray(new String[captureNames.size()]);
        final List<ParameterType> parameterTypes = route.captureParameterTypes();
        final ParameterType[] paramTypes =
            parameterTypes.toArray(new ParameterType[parameterTypes.size()]);

        final Target<T> target = new Target<T>(this.target, paramNames, paramTypes);
        if (currentValue == null) {
          return RouteTarget.of(route.method(), target);
        }
        return currentValue.with(route.method(), target);
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
    private RouteTarget<T> route;
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
     * Get the parameter type of the captured path parameter at index {@code i}.
     */
    public ParameterType paramType(final int i) {
      if (target == null) {
        throw new IllegalStateException("not matched");
      }

      return target.paramTypes[i];
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
     * Returns the index for a given parameter name, if it exists.
     * @param paramName The name of the param
     * @return An {@link OptionalInt} representing the index.
     */
    private OptionalInt paramIndex(String paramName) {
      for (int i = 0; i < params(); i++) {
        if (paramName(i).equals(paramName)) {
          return OptionalInt.of(i);
        }
      }
      return OptionalInt.empty();
    }

    /**
     * Returns the index for a given parameter name, if it exists.
     * @param paramName The name of the param
     * @return An {@link OptionalInt} representing the index.
     * @throws RuntimeException if there is no parameter for that name.
     */
    private int paramIndexOrThrow(String paramName) {
      return paramIndex(paramName)
              .orElseThrow(() -> new RuntimeException("No parameter: " + paramName));
    }

    /**
     * Get the value of the captured path parameter.
     *
     * @param paramName The name of the parameter.
     */
    public CharSequence paramValue(final String paramName) {
      return paramValue(paramIndexOrThrow(paramName));
    }

    /**
     * Get the URL decoded value of the captured path parameter.
     *
     * @param paramName The name of the parameter.
     * @return The decoded value or null if the encoding is invalid.
     */
    public CharSequence paramValueDecoded(final String paramName) {
      return paramValueDecoded(paramIndexOrThrow(paramName));
    }

    /**
     * Get the parameter type of the captured path parameter .
     *
     * @param paramName The name of the parameter.
     */
    public ParameterType paramType(final String paramName) {
      return paramType(paramIndexOrThrow(paramName));
    }

    /**
     * Get start offset into the routed path of the captured parameter.
     *
     * @param paramName The name of the parameter.
     * @see #paramValue
     * @see #paramValueEnd
     */
    public int paramValueStart(final String paramName) {
      return paramValueStart(paramIndexOrThrow(paramName));
    }

    /**
     * Get end offset into the routed path of the captured parameter.
     *
     * @param paramName The name of the parameter.
     * @see #paramValue
     * @see #paramValueStart
     */
    public int paramValueEnd(final String paramName) {
      return paramValueEnd(paramIndexOrThrow(paramName));
    }

    /**
     * Signal a route found but method not allowed.
     */
    private Result<T> notAllowed(final RouteTarget<T> route) {
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
    private Result<T> success(final CharSequence path, final RouteTarget<T> route,
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
  private static class RouteTarget<T> {

    private final String method;
    private final Target<T> target;
    private final RouteTarget<T> next;
    private final Collection<String> methods;

    private RouteTarget(final String method, final Target<T> target, final RouteTarget<T> next) {
      this.method = method;
      this.target = target;
      this.next = next;
      this.methods = methods0();
    }

    /**
     * Create a new route.
     */
    private static <T> RouteTarget<T> of(final String method, final Target<T> target) {
      return new RouteTarget<T>(method, target, null);
    }

    /**
     * Add a new method and target to this route.
     */
    private RouteTarget<T> with(final String method, final Target<T> target) {
      return new RouteTarget<T>(method, target, this);
    }

    /**
     * Look up a method in this route.
     *
     * @return The endpoint if the method matched. {@code null} otherwise.
     */
    private Target<T> lookup(final CharSequence method) {
      RouteTarget<T> route = this;
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
      RouteTarget<T> route = this;
      while (route != null) {
        methods.add(route.method);
        route = route.next;
      }
      return Collections.unmodifiableList(methods);
    }
  }
}
