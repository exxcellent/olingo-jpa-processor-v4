package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class FieldAttributeAccessor implements JPAAttributeAccessor {

	private final Field field;

	public FieldAttributeAccessor(final Field field) {
		this.field = field;
	}

	@Override
	public Object getDefaultPropertyValue() throws ODataJPAModelException {
		// It is not possible to get the default value directly from the Field,
		// only from an instance field.get(Object obj).toString();
		try {
			// FIXME
			// final Constructor<?> constructor = jpaAttribute.getDeclaringType().getJavaType().getConstructor();
			final Constructor<?> constructor = field.getDeclaringClass().getConstructor();
			final Object pojo = constructor.newInstance();
			return getPropertyValue(pojo);
		} catch (final InstantiationException | NoSuchMethodException e) {
			// Class could not be instantiated e.g. abstract class like Business Partner=>
			// default could not be determined
			// and will be ignored
		} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.PROPERTY_DEFAULT_ERROR, e,
			        field.getName());
		}
		return null;
	}

	@Override
	public void setPropertyValue(final Object jpaEntity, final Object jpaPropertyValue) throws ODataJPAModelException {
		// don't try to set null values to primitive fields
		if (jpaPropertyValue == null && field.getType().isPrimitive())
			return;
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
