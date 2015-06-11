package io.norberg.rut;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static io.norberg.rut.ParameterType.PATH;
import static io.norberg.rut.ParameterType.SEGMENT;
import static io.norberg.rut.Router.Status.METHOD_NOT_ALLOWED;
import static io.norberg.rut.Router.Status.NOT_FOUND;
import static io.norberg.rut.Router.Status.SUCCESS;
import static java.lang.Character.toChars;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class RouterTest {

  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void testEmptyRouter() {
    final Router<String> router = Router.builder(String.class).build();
    final Router.Result<String> result = router.result();
    Router.Status status = router.route("GET", "foobar", result);
    assertThat(status, is(NOT_FOUND));
    assertThat(result.isSuccess(), is(false));
    assertThat(result.status(), is(NOT_FOUND));
  }

  @Test
  public void testRouting() {
    final String getTarget = "target";
    final String postTarget = "target";

    final Router<String> router = Router.builder(String.class)
        .route("GET", "/foo/<bar>/baz", getTarget)
        .route("POST", "/foo/<bar>/baz", postTarget)
        .build();

    final Router.Result<String> result = router.result();

    final Router.Status status1 = router.route("GET", "/foo/bar-value/baz?q=a&w=b", result);
    assertThat(status1, is(Router.Status.SUCCESS));
    assertThat(result.status(), is(Router.Status.SUCCESS));
    assertThat(result.isSuccess(), is(true));
    assertThat(result.target(), is(getTarget));
    assertThat(result.queryStart(), is(19));
    assertThat(result.queryEnd(), is(26));
    assertThat(result.query().toString(), is("q=a&w=b"));
    assertThat(result.params(), is(1));
    assertThat(result.paramValueStart(0), is(5));
    assertThat(result.paramValueEnd(0), is(14));
    assertThat(result.allowedMethods(), hasSize(2));
    assertThat(result.allowedMethods(), containsInAnyOrder("GET", "POST"));
    final String name = result.paramName(0);
    final CharSequence value = result.paramValue(0);
    assertThat(name, is("bar"));
    assertThat(value.toString(), is("bar-value"));

    final Router.Status status2 = router.route("DELETE", "/foo/bar/baz", result);
    assertThat(status2, is(METHOD_NOT_ALLOWED));
    assertThat(result.status(), is(METHOD_NOT_ALLOWED));
    assertThat(result.allowedMethods(), hasSize(2));
    assertThat(result.allowedMethods(), containsInAnyOrder("GET", "POST"));
    assertThat(result.isSuccess(), is(false));

    final Router.Status status3 = router.route("GET", "/non/existent/path", result);
    assertThat(status3, is(Router.Status.NOT_FOUND));
    assertThat(result.status(), is(Router.Status.NOT_FOUND));
    assertThat(result.isSuccess(), is(false));
    try {
      result.allowedMethods();
      fail();
    } catch (IllegalStateException ignored) {
    }
  }

  @Test
  public void verifyResultTargetThrowsIfNotSuccessful() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "/foo", "foo")
        .build();

    final Router.Result<String> result = router.result();
    router.route("GET", "/bar", result);

    exception.expect(IllegalStateException.class);

    result.target();
  }


  @Test
  public void verifyResultParamNameThrowsIfNotSuccessful() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "/foo", "foo")
        .build();

    final Router.Result<String> result = router.result();
    router.route("GET", "/bar", result);

    exception.expect(IllegalStateException.class);

    result.paramName(0);
  }

  @Test
  public void testRouterBuilderWithoutClassArgument() {
    final Router.Builder<String> b = Router.builder();
    b.route("GET", "foo", "foo");
    b.build();
  }

  @Test
  public void verifyWrongMethodNotAllowed() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "/foo", "foo-get")
        .route("POST", "/foo", "foo-post")
        .build();
    final Router.Result<String> result = router.result();

    assertThat(router.route("PUT", "/foo", result), is(METHOD_NOT_ALLOWED));
    assertThat(result.status(), is(METHOD_NOT_ALLOWED));
    assertThat(result.allowedMethods(), hasSize(2));
    assertThat(result.allowedMethods(), containsInAnyOrder("GET", "POST"));

    assertThat(router.route("GGG", "/foo", result), is(METHOD_NOT_ALLOWED));
    assertThat(result.status(), is(METHOD_NOT_ALLOWED));
    assertThat(result.allowedMethods(), hasSize(2));
    assertThat(result.allowedMethods(), containsInAnyOrder("GET", "POST"));
  }

  @SuppressWarnings("RedundantStringConstructorCall")
  @Test
  public void verifyNonIdenticalStringMatches() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "/foo", "/foo")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route(String.valueOf(new char[]{'G', 'E', 'T'}),
                            String.valueOf(new char[]{'/', 'f', 'o', 'o'}), result),
               is(SUCCESS));
  }

  @Test
  public void testParamValueDecoding() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "r%c3%A4k%20sm%C3%B6rg%C3%A5s", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("r%c3%A4k%20sm%C3%B6rg%C3%A5s"));
    assertThat(result.paramValueDecoded(0).toString(), is("räk smörgås"));
  }

  @Test
  public void testParamValueDecodingInvalidInitialFirstNibble() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "%g3", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%g3"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));

    assertThat(router.route("GET", "%+3", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%+3"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));

    assertThat(router.route("GET", "%@3", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%@3"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));

    assertThat(router.route("GET", "%[3", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%[3"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));
  }

  @Test
  public void testParamValueDecodingInvalidInitialSecondNibble() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "%fo", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%fo"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));
  }

  @Test
  public void testParamValueDecodingInvalidTooShort() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "%C", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%C"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));
  }

  @Test
  public void testParamValueDecodingInvalidSecondChar() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "%C3%fo", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("%C3%fo"));
    assertThat(result.paramValueDecoded(0), is(nullValue()));
  }

  @Test
  public void testParamValueDecodingUnencoded() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "foobar", result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is("foobar"));
    assertThat(result.paramValueDecoded(0).toString(), is("foobar"));
  }

  @Test
  public void testParamValueDecodingSupplementary() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    final String decoded = new String(toChars(0x2FA1B));
    final String encoded = "%F0%AF%A8%9B";
    assertThat(router.route("GET", encoded, result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is(encoded));
    assertThat(result.paramValueDecoded(0).toString(), is(decoded));
  }

  @Test
  public void testParamValueDecodingSupplementaryComplex() {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "<param>", "")
        .build();
    final Router.Result<String> result = router.result();
    final String decoded = new String(toChars(0x2FA1B)) + new String(toChars(0x2F8D3));
    final String encoded = "%F0%AF%A8%9B%F0%AF%A3%93";
    final String path = "foo-" + encoded + "-bar-" + encoded;
    assertThat(router.route("GET", path, result), is(SUCCESS));
    assertThat(result.paramValue(0).toString(), is(path));
    assertThat(result.paramValueDecoded(0).toString(), is("foo-" + decoded + "-bar-" + decoded));
  }

  private void assertSucc(final Router<String> r, final String m,
                          final String u, final String t, final List<String> p) {
    assertSucc(r, m, u, t, p, null);
    for (int i = 0; i < 32; i++) {
      final String query = repeat("q", i);
      assertSucc(r, m, u + "?" + query, t, p, query);
    }
  }

  private <T> void assertSucc(final Router<T> router, final String method,
                              final String uri, final T target, final List<String> params,
                              final String query) {
    final Router.Result<T> result = router.result();
    assertThat(router.route(method, uri, result), is(SUCCESS));
    assertThat(result.target(), is(target));
    assertThat(result.params(), is(params.size()));
    assertThat(result.isSuccess(), is(true));
    for (int i = 0; i < params.size(); i++) {
      assertThat(result.paramValue(i).toString(), is(params.get(i)));
    }
    assertThat(toString(result.query()), is(query));
  }

  private void assertFail(final Router<String> r, final String m, final String u) {
    assertNotFound(r, m, u);
    for (int i = 0; i < 32; i++) {
      assertNotFound(r, m, u + "?" + repeat("q", i));
    }
  }

  private <T> void assertNotFound(final Router<T> r, final String m, final String u) {
    final Router.Result<T> result = r.result();
    assertThat(r.route(m, u, result), is(NOT_FOUND));
    assertThat(result.params(), is(0));
    assertThat(result.isSuccess(), is(false));
    assertThat(result.query(), is(nullValue()));
    assertThat(result.queryStart(), is(-1));
    assertThat(result.queryEnd(), is(-1));
    try {
      result.allowedMethods();
      fail();
    } catch (IllegalStateException ignore) {
    }
    try {
      result.target();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  private String toString(final CharSequence s) {
    return s == null ? null : s.toString();
  }

  @Test
  public void testOptionalTrailingSlash() {
    final Router<String> r = Router.builder(String.class)
        .optionalTrailingSlash(true)
        .route("GET", "/1-without-trailing-slash", "1")
        .route("GET", "/2-without-trailing-slash/<param>", "2")
        .route("GET", "/3-with-trailing-slash/", "3")
        .route("GET", "/4-with-trailing-slash/<param>/", "4")
        .route("GET", "/5-without-trailing-slash-path/<param:path>", "5")
        .route("GET", "/6-with-trailing-slash-nested/entity/", "6")
        .route("GET", "/7-with-trailing-slash-nested/entity/<param>", "7")
        .route("GET", "/8-without-trailing-slash-nested/entity", "8")
        .route("GET", "/9-without-trailing-slash-nested/entity/<param>", "9")
        .route("GET", "/1x-ambiguous", "10")
        .route("GET", "/1x-ambiguous/", "11")
        .route("GET", "/2x-fork", "20")
        .route("GET", "/2x-fork/", "21")
        .route("GET", "/2x-fork/a", "22")
        .route("GET", "/2x-fork/b", "23")
        .route("GET", "/2x-fork/<param>", "24")
        .route("GET", "/", "25")
        .route("GET", "//", "26")
        .route("GET", "//<param:path>", "27")
        .build();

    assertSucc(r, "GET", "/1-without-trailing-slash", "1", p());
    assertSucc(r, "GET", "/1-without-trailing-slash/", "1", p());

    assertSucc(r, "GET", "/2-without-trailing-slash/foo", "2", p("foo"));
    assertSucc(r, "GET", "/2-without-trailing-slash/foo/", "2", p("foo"));

    assertSucc(r, "GET", "/3-with-trailing-slash", "3", p());
    assertSucc(r, "GET", "/3-with-trailing-slash/", "3", p());
    assertFail(r, "GET", "/3-with-trailing-slash-no-match");

    assertSucc(r, "GET", "/4-with-trailing-slash/foo", "4", p("foo"));
    assertSucc(r, "GET", "/4-with-trailing-slash/foo/", "4", p("foo"));
    assertFail(r, "GET", "/4-with-trailing-slash-no-match/foo");

    assertSucc(r, "GET", "/5-without-trailing-slash-path/foo", "5", p("foo"));
    assertSucc(r, "GET", "/5-without-trailing-slash-path/foo/bar", "5", p("foo/bar"));

    assertSucc(r, "GET", "/6-with-trailing-slash-nested/entity", "6", p());
    assertSucc(r, "GET", "/6-with-trailing-slash-nested/entity/", "6", p());
    assertFail(r, "GET", "/6-with-trailing-slash-nested/no-match");
    assertFail(r, "GET", "/6-with-trailing-slash-nested-no-match");
    assertFail(r, "GET", "/6-with-trailing-slash-nested/entity-no-match");
    assertFail(r, "GET", "/6-with-trailing-slash-nested/entity/no-match");

    assertSucc(r, "GET", "/7-with-trailing-slash-nested/entity/foo", "7", p("foo"));
    assertFail(r, "GET", "/7-with-trailing-slash-nested");
    assertFail(r, "GET", "/7-with-trailing-slash-nested/entity-no-match");

    assertSucc(r, "GET", "/8-without-trailing-slash-nested/entity", "8", p());
    assertFail(r, "GET", "/8-without-trailing-slash-nested/entity-no-match");
    assertFail(r, "GET", "/8-without-trailing-slash-nested/entity/no-match");
    assertFail(r, "GET", "/8-without-trailing-slash-nested/");
    assertFail(r, "GET", "/8-without-trailing-slash-nested");

    assertSucc(r, "GET", "/9-without-trailing-slash-nested/entity/foo", "9", p("foo"));
    assertFail(r, "GET", "/9-without-trailing-slash-nested/entity-no-match");
    assertFail(r, "GET", "/9-without-trailing-slash-nested/entity/foo/no-match");
    assertFail(r, "GET", "/9-without-trailing-slash-nested/entity");
    assertFail(r, "GET", "/9-without-trailing-slash-nested/entity/");

    assertSucc(r, "GET", "/1x-ambiguous", "10", p());
    assertFail(r, "GET", "/1x-ambiguous-no-match");
    assertSucc(r, "GET", "/1x-ambiguous/", "11", p());
    assertFail(r, "GET", "/1x-ambiguous/no-match");

    assertSucc(r, "GET", "/2x-fork", "20", p());
    assertSucc(r, "GET", "/2x-fork/", "21", p());
    assertSucc(r, "GET", "/2x-fork/a", "22", p());
    assertSucc(r, "GET", "/2x-fork/b", "23", p());
    assertSucc(r, "GET", "/2x-fork/foo", "24", p("foo"));
    assertSucc(r, "GET", "/2x-fork/afoo", "24", p("afoo"));
    assertSucc(r, "GET", "/2x-fork/bfoo", "24", p("bfoo"));

    assertSucc(r, "GET", "/", "25", p());
    assertSucc(r, "GET", "//", "26", p());
    assertSucc(r, "GET", "///", "27", p("/"));
    assertSucc(r, "GET", "//foo", "27", p("foo"));
    assertSucc(r, "GET", "//foo/bar", "27", p("foo/bar"));
  }

  /**
   * Dirty tests that know too much about the internals.
   */
  @Test
  public void testOptionalTrailingSlashEdgeCases() {
    final Router<String> r = Router.builder(String.class)
        .optionalTrailingSlash(true)
        .route("GET", "1-shortfork1/1", "")
        .route("GET", "1-shortfork1/2", "")
        .route("GET", "1-shortfork1-", "")
        .route("GET", "2-longfork1/tail", "")
        .route("GET", "2-longfork1atail", "")
        .route("GET", "3-foo/", "")
        .route("GET", "4-foo", "")
        .route("GET", "5-foo/", "5")
        .route("GET", "5-foo_", "")
        .build();

    assertFail(r, "GET", "1-shortfork1");
    assertFail(r, "GET", "2-longfork1");
    assertFail(r, "GET", "3-f");
    assertFail(r, "GET", "4-fo_");
    assertSucc(r, "GET", "5-foo", "5", p());
  }

  @Test
  public void testParamTypes() throws Exception {
    final Router<String> router = Router.builder(String.class)
        .route("GET", "/<param>/<end:path>", "")
        .build();
    final Router.Result<String> result = router.result();
    assertThat(router.route("GET", "/foobar/the/end", result), is(SUCCESS));
    assertThat(result.paramType(0), is(SEGMENT));
    assertThat(result.paramType(1), is(PATH));
  }

  private List<String> p(final String... params) {
    return asList(params);
  }

  private String repeat(final String s, final int n) {
    final StringBuilder b = new StringBuilder(s.length() * n);
    for (int i = 0; i < n; i++) {
      b.append(s);
    }
    return b.toString();
  }
}
