package dano;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.regex.Pattern;

@State(Scope.Thread)
public class RegexBenchmark {

  private static final String[] PATHS = {
      "/usercount",
      "/users",
      "/users/<user>",
      "/users/<user>/playlistcount",
      "/users/<user>/playlists",
      "/users/<user>/playlists/<playlist>/itemcount",
      "/users/<user>/playlists/<playlist>/items",
      "/users/<user>/playlists/<playlist>/items/<item>",
      "/users/<user>/playlists/<playlist>"
  };

  private static final Router<String> ROUTER;
  private static final Router.Result<String> RESULT;

  static {
    final Router.Builder<String> builder = Router.builder();
    for (final String path : PATHS) {
      builder.route("GET", path, path);
    }
    ROUTER = builder.build();
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

  private static final String PATH = "/users/foo-user/playlists/bar-playlist";

  private String path;
  private Pattern[] uriPatterns;

  @Setup
  public void setup() {
    path = PATH;
    uriPatterns = PATTERNS;
  }

  @Benchmark
  public Pattern benchRegexRouting() throws InterruptedException {
    for (final Pattern pattern : uriPatterns) {
      if (pattern.matcher(path).matches()) {
        return pattern;
      }
    }
    throw new AssertionError();
  }

  @Benchmark
  public String benchRadixTreeRouting() {
    ROUTER.route("GET", path, RESULT);
    final String target = RESULT.target();
    if (target == null) {
      throw new AssertionError();
    }
    return target;
  }

  public static void main(final String... args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(".*" + RegexBenchmark.class.getSimpleName() + ".*")
        .warmupIterations(5)
        .measurementIterations(20)
        .forks(5)
        .build();

    new Runner(opt).run();
  }

}
