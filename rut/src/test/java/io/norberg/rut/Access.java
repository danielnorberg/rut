package io.norberg.rut;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class Access {

  static void verifyUninstantiable(final Class<?> cls) {
    final Constructor<?> constructor = cls.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    try {
      constructor.newInstance();
      throw new AssertionError();
    } catch (InstantiationException ignored) {
      throw new AssertionError();
    } catch (IllegalAccessException ignored) {
      throw new AssertionError();
    } catch (IllegalArgumentException ignored) {
      throw new AssertionError();
    } catch (InvocationTargetException ignored) {
    }
  }
}
