package io.norberg.rut.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.regex.Pattern;

import io.norberg.rut.Router;

@State(Scope.Thread)
public class RoutingBenchmark {

  private static final String[] PATHS = {
      "/users/",
      "/users/<user>",
      "/users/<user>/profile",
      "/users/<user>/blogs/",
      "/users/<user>/blogs/<blog>/posts/",
      "/users/<user>/blogs/<blog>/posts/<post>",
      "/users/<user>/blogs/<blog>",
      "/blogs/",
      "/blogs/<blog>",
  };

  private static final String PATH = "/users/foo-user/blogs/bar-blog/posts/baz-post";
  private static final String NOT_FOUND_PATH = "/users/foo-user/blogs/bar-blog/followers/foo";

  private static final Router<String> ROUTER;
  private static final Router<String> ROUTER_OPTIONAL_TRAILING_SLASH;
  private static final Router.Result<String> RESULT;

  static {
    final Router.Builder<String> builder = Router.builder();
    for (final String path : PATHS) {
      builder.route("GET", path, path);
    }
    ROUTER = builder.build();
    builder.optionalTrailingSlash(true);
    ROUTER_OPTIONAL_TRAILING_SLASH = builder.build();
    RESULT = ROUTER.result();
  }

  private static final Pattern[] PATTERNS;

  static {
    PATTERNS = new Pattern[PATHS.length];
    for (int i = 0; i < PATHS.length; i++) {
      final String path = PATHS[i];
      final String regex = path.replaceAll("<.+?>", "[^/]*");
      PATTERNS[i] = Pattern.compile(regex);
    }
  }

  private String path;
  private String notFoundPath;
  private Pattern[] uriPatterns;

  @Setup
  public void setup() {
    path = PATH;
    notFoundPath = NOT_FOUND_PATH;
    uriPatterns = PATTERNS;
  }

  @Benchmark
  public Pattern regexRouting() {
    for (final Pattern pattern : uriPatterns) {
      if (pattern.matcher(path).matches()) {
        return pattern;
      }
    }
    throw new AssertionError();
  }

  @Benchmark
  public Pattern regexRoutingNotFound() {
    for (final Pattern pattern : uriPatterns) {
      if (pattern.matcher(notFoundPath).matches()) {
        return pattern;
      }
    }
    return null;
  }

  @Benchmark
  public String radixTreeRouting() {
    ROUTER.route("GET", path, RESULT);
    final String target = RESULT.target();
    if (target == null) {
      throw new AssertionError();
    }
    return target;
  }

  @Benchmark
  public String radixTreeRoutingNotFound() {
    ROUTER.route("GET", notFoundPath, RESULT);
    if (RESULT.isSuccess()) {
      return RESULT.target();
    }
    return null;
  }

  @Benchmark
  public String radixTreeRoutingWithOptionalTrailingSlash() {
    ROUTER_OPTIONAL_TRAILING_SLASH.route("GET", path, RESULT);
    final String target = RESULT.target();
    if (target == null) {
      throw new AssertionError();
    }
    return target;
  }

  public static void main(final String... args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(".*" + RoutingBenchmark.class.getSimpleName() + ".*")
        .warmupIterations(5)
        .measurementIterations(20)
        .forks(5)
        .build();

    new Runner(opt).run();
  }
}
