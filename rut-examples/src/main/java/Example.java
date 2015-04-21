import io.norberg.rut.Router;

import static java.lang.System.out;

public class Example {

  public interface Handler {

  }

  public static void main(final String... args) {

    // Set up router
    final Router<Handler> router = Router.builder(Handler.class)
        .route("GET", "/users/", handler("list users"))
        .route("POST", "/users/", handler("create user"))
        .route("GET", "/users/<user>/blogs/", handler("list user blogs"))
        .route("POST", "/users/<user>/blogs/", handler("create user blog"))
        .route("GET", "/users/<user>/blogs/<blog>/posts/", handler("list user blog posts"))
        .route("POST", "/users/<user>/blogs/<blog>/posts/", handler("create user blog post"))
        .route("GET", "/users/<user>/blogs/<blog>/posts/<post>", handler("get user blog post"))
        .route("GET", "/static/<filename:path>", handler("get static content"))
        .build();

    // Create a reusable routing result holder
    final Router.Result<Handler> result = router.result();

    // Route a request
    router.route("POST", "/users/foo-user/blogs/bar-blog/posts/?q=baz&w=quux", result);

    assert result.isSuccess();
    final Handler handler = result.target();

    // Print handler name
    out.println("handler: " + handler);

    // Print captured path parameter names and values
    for (int i = 0; i < result.params(); i++) {
      out.printf("param %d: %s=%s%n", i, result.paramName(i), result.paramValue(i));
    }

    // Print query
    out.println("query: " + result.query());

    // List all allowed methods
    out.println("allowed methods: " + result.allowedMethods());
  }

  private static Handler handler(final String description) {
    return new Handler() {

      @Override
      public String toString() {
        return description;
      }
    };
  }
}
