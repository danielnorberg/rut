package io.norberg.rut;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class EncodingTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void verifyUninstantiable()
      throws IllegalAccessException, InstantiationException, InvocationTargetException {
    final Constructor<?> constructor = Encoding.class.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    exception.expect(InvocationTargetException.class);
    constructor.newInstance();
  }
}