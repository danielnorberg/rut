package io.norberg.rut;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PathTest {

  @Test(expected = IllegalArgumentException.class)
  public void verifyEmptyListOfPartsThrows() {
    new Path(new ArrayList<Path.Part>());
  }

  @Test
  public void verifyToStringMatchesOriginal() {
    final String pathString = "/foo/<bar>/baz/<quux:path>";
    final Path path = Path.of(pathString);
    assertThat(path.toString(), is(pathString));
  }
}
