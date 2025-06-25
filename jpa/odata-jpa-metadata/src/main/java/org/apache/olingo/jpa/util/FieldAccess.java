package org.apache.olingo.jpa.util;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldAccess {
  public static Object readFieldValue(final Object object, final Field field)
      throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    boolean revertAccessibility = false;
    if (!field.isAccessible()) {
      field.setAccessible(true);
      revertAccessibility = true;
    }
    final Object value = field.get(object);
    if (revertAccessibility) {
      field.setAccessible(false);
    }
    return value;

  }

  public static void writeFieldValue(final Object targetObject, final Field field, final Object fieldValue)
      throws IllegalArgumentException, IllegalAccessException {
    boolean revertAccessibility = false;
    if (!field.isAccessible()) {
      field.setAccessible(true);
      revertAccessibility = true;
    }
    // 'Set' is also handled as collection, because correct type must be created outside...
    if (Collection.class.isAssignableFrom(field.getType()) && Collection.class.isInstance(fieldValue)
        && field.get(targetObject) != null) {
      // do not set the collection directly, because some specific implementations may
      // cause problems... add entries in collection instead
      @SuppressWarnings("unchecked")
      final Collection<Object> target = (Collection<Object>) field.get(targetObject);
      target.clear();
      @SuppressWarnings("unchecked")
      final Collection<Object> source = (Collection<Object>) fieldValue;
      target.addAll(source);
    } else {
      // replace 'null' value with our collection (List, Set)
      field.set(targetObject, fieldValue);
    }
    if (revertAccessibility) {
      field.setAccessible(false);
    }
  }

}
