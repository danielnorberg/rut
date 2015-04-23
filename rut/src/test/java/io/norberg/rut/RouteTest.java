package io.norberg.rut;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RouteTest {

  @Test
  public void testMatch() {
    final Route r = Route.of("GET", "foo");
    assertThat(r.captureNames(), is(empty()));
  }

  @Test
  public void testSeg() {
    final Route r = Route.of("GET", "<param>");
    assertThat(r.captureNames(), contains("param"));
  }

  @Test
  public void testSegMatch() {
    final Route r = Route.of("GET", "<param>foo");
    assertThat(r.captureNames(), contains("param"));
  }

  @Test
  public void testMatchSeg() {
    final Route r = Route.of("GET", "foo<param>");
    assertThat(r.captureNames(), contains("param"));
  }

  @Test
  public void testMatchSegMatchSeg() {
    final Route r = Route.of("GET", "foo<param1>bar<param2>");
    assertThat(r.captureNames(), contains("param1", "param2"));
  }

  @Test
  public void testMatchSegMatchSegMatch() {
    final Route r = Route.of("GET", "foo<param1>bar<param2>baz");
    assertThat(r.captureNames(), contains("param1", "param2"));
  }

  @Test
  public void testMethod() {
    assertThat(Route.of("GET", "/foo").method(), is("GET"));
    assertThat(Route.of("FOOBAR", "/foo").method(), is("FOOBAR"));
  }

  @Test
  public void testPathString() {
    final String path = "/foo/<bar>/baz/<quux:path>";
    assertThat(Route.of("GET", path).pathString(), is(path));
  }

  @Test
  public void testToString() {
    final String path = "/foo/<bar>/baz/<quux:path>";
    final String method = "GET";
    assertThat(Route.of(method, path).toString(), is(method + " " + path));
  }
}
