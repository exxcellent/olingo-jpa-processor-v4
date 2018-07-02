package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.util.Collection;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class FieldAttributeAccessor implements JPAAttributeAccessor {

	private final Field field;

	public FieldAttributeAccessor(final Field field) {
		this.field = field;
	}

	@Override
	public void setPropertyValue(final Object jpaEntity, final Object jpaPropertyValue) throws ODataJPAModelException {
		try {
			writeJPAFieldValue(jpaEntity, field, jpaPropertyValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ODataJPAModelException(e);
		}
	}

	@Override
	public Object getPropertyValue(final Object jpaEntity) throws ODataJPAModelException {
		try {
			return readJPAFieldValue(jpaEntity, field);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			throw new ODataJPAModelException(e);
		}
	}

	private static Object readJPAFieldValue(final Object object, /* final Class<?> jpaClassType, */ final Field field)
			throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		boolean revertAccessibility = false;
		// try {
		// final Field field = jpaClassType.getDeclaredField(fieldName);
		if (!field.isAccessible()) {
			field.setAccessible(true);
			revertAccessibility = true;
		}
		final Object value = field.get(object);
		if (revertAccessibility) {
			field.setAccessible(false);
		}
		return value;

		// } catch (final NoSuchFieldException ex) {
		// if (jpaClassType.getSuperclass() != null) {
		// return readJPAFieldValue(object, jpaClassType.getSuperclass(), fieldName);
		// }
		// // if not found in super classes, throw the exception
		// throw ex;
		// }
	}

	private static void writeJPAFieldValue(final Object jpaEntity, final Field field, final Object jpaPropertyValue)
			throws IllegalArgumentException, IllegalAccessException {
		boolean revertAccessibility = false;
		if (!field.isAccessible()) {
			field.setAccessible(true);
			revertAccessibility = true;
		}
		if (Collection.class.isAssignableFrom(field.getType()) && Collection.class.isInstance(jpaPropertyValue)
				&& field.get(jpaEntity) != null) {
			// do not set the collection directly, because some specific implementations may
			// cause problems... add entries in collection instead
			@SuppressWarnings("unchecked")
			final Collection<Object> target = (Collection<Object>) field.get(jpaEntity);
			@SuppressWarnings("unchecked")
			final Collection<Object> source = (Collection<Object>) jpaPropertyValue;
			target.addAll(source);
		} else {
			field.set(jpaEntity, jpaPropertyValue);
		}
		if (revertAccessibility) {
			field.setAccessible(false);
		}
	}

}
