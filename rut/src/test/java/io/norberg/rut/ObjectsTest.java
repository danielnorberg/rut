package io.norberg.rut;

import org.junit.Test;

import static io.norberg.rut.Objects.requireNonNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ObjectsTest {

  @Test(expected = NullPointerException.class)
  public void verifyRequireNonNullThrowsOnNull() throws Exception {
    requireNonNull(null, "foo");
  }

  @Test
  public void verifyRequireNonNullReturnsValue() throws Exception {
    final String value = "foo";
    final String returned = requireNonNull(value, "foo");
    assertThat(returned, is(value));
  }

  @Test
  public void verifyUninstantiable() {
    Access.verifyUninstantiable(Objects.class);
  }
}
