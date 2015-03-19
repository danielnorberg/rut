rut
===

Pronounced as *route* (/ru:t/).

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.norberg/rut/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.norberg/rut)

[![Build Status](https://travis-ci.org/danielnorberg/rut.svg?branch=master)](https://travis-ci.org/danielnorberg/rut)

[![Coverage Status](https://coveralls.io/repos/danielnorberg/rut/badge.svg?branch=master)](https://coveralls.io/r/danielnorberg/rut?branch=master)

[![docs examples](https://sourcegraph.com/api/repos/github.com/danielnorberg/rut/.badges/docs-examples.svg)](https://sourcegraph.com/github.com/danielnorberg/rut)

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
```

Output: 
```
handler: create user blog post
param 0: user=foo-user
param 1: blog=bar-blog
query: q=baz&w=quux
```

### `pom.xml`

```xml
<dependency>
  <groupId>io.norberg</groupId>
  <artifactId>rut</artifactId>
  <version>0.5</version>
</dependency>
```

Notes
-----

* *rut* only handles ascii. Paths should be URL encoded when routed.


Benchmarks
----------

```
mvn clean package

java -jar rut-benchmark/target/rut-benchmark.jar
```

```
Benchmark                           Mode  Cnt         Score        Error  Units
RoutingBenchmark.radixTreeRouting  thrpt  200  10394965.771 ± 107132.302  ops/s
RoutingBenchmark.regexRouting      thrpt  200    911995.189 ±   5912.181  ops/s
```

