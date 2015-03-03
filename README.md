rut
===

Pronounced as *route* (/ru:t/).


What
----

A request router that attempts to be fast by using a radix tree internally and avoiding object
allocation.


Why
---

The frequently recurring pattern of routing by iterating over a list of compiled regex patterns is
not always a great performer.

*rut* aims to be at least an order of magnitude faster.


Usage
-----

```java
// From rut-examples/src/main/java/Example.java

// Set up router
final Router<Handler> router = Router.builder(Handler.class)
    .route("GET", "/users/", handler("list users"))
    .route("POST", "/users/", handler("create user"))
    .route("GET", "/users/<user>/blogs/", handler("list user blogs"))
    .route("POST", "/users/<user>/blogs/", handler("create user blog"))
    .route("GET", "/users/<user>/blogs/<blog>/posts/", handler("list user blog posts"))
    .route("POST", "/users/<user>/blogs/<blog>/posts/", handler("create user blog post"))
    .route("GET", "/users/<user>/blogs/<blog>/posts/<post>", handler("get user blog post"))
    .build();

// Create a reusable routing result holder
final Router.Result<Handler> result = router.result();

// Route a request
router.route("POST", "/users/foo-user/blogs/bar-blog/posts/", result);

assert result.isSuccess();
final Handler handler = result.target();

// Print handler name
out.print(handler + ": ");

// Print captured path parameter names and values
for (int i = 0; i < result.params(); i++) {
  out.print(result.paramName(i) + "=" + result.paramValue(i) + " ");
}
```

Output: `create user blog post: user=foo-user blog=bar-blog`

### `pom.xml`

```xml
<dependency>
  <groupId>io.norberg</groupId>
  <artifactId>rut</artifactId>
  <version>0.3</version>
</dependency>
```

Notes
-----

* *rut* only handles ascii. Paths should thus be URL encoded when routed.
