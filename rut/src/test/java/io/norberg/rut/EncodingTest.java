package io.norberg.rut;

import org.junit.Test;

public class EncodingTest {

  @Test
  public void verifyUninstantiable() {
    Access.verifyUninstantiable(Encoding.class);
  }
}
