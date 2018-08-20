package org.apache.olingo.jpa.processor.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Helper class to realize a limited support for dependency injection. Supported
 * are:
 * <ul>
 * <li>org.apache.olingo.jpa.cdi.Inject (javax.inject.Inject): for fields</li>
 * <li>org.apache.olingo.jpa.cdi.Inject: for method parameters</li>
 * </ul>
 *
 * @author Ralf Zozmann
 *
 */
@SuppressWarnings("unchecked")
public final class DependencyInjector {

	private static final String JAVAX_INJECT_INJECT_CLASSNAME = "javax.inject.Inject";
	private static final Collection<Class<?>> FORBIDDEN_TYPES = new LinkedList<>();
	private static Class<? extends Annotation> injectAnnotation;

	static {
		FORBIDDEN_TYPES.add(Boolean.class);
		FORBIDDEN_TYPES.add(Character.class);
		FORBIDDEN_TYPES.add(Byte.class);
		FORBIDDEN_TYPES.add(Short.class);
		FORBIDDEN_TYPES.add(Integer.class);
		FORBIDDEN_TYPES.add(Long.class);
		FORBIDDEN_TYPES.add(Float.class);
		FORBIDDEN_TYPES.add(Double.class);
		FORBIDDEN_TYPES.add(Void.class);
		try {
			injectAnnotation = (Class<? extends Annotation>) Class.forName(JAVAX_INJECT_INJECT_CLASSNAME);
		} catch (final ClassNotFoundException e) {
			injectAnnotation = null;
		}
	}

	private static class InjectionOccurrence {
		private final Field field;
		private final Object matchingObject;

		InjectionOccurrence(final Field field, final Object matchingObject) {
			super();
			this.field = field;
			this.matchingObject = matchingObject;
		}
	}

	private final Map<Class<?>, Object> valueMapping = new HashMap<>();

	/**
	 *
	 * @param type
	 *            The type as registered via
	 *            {@link #registerDependencyMapping(Class, Object)}.
	 * @return The value for registered type.
	 */
	public Object getDependencyValue(final Class<?> type) {
		return valueMapping.get(type);
	}

	/**
	 * Register a value to inject into {@link #injectFields(Object) targets}.
	 *
	 * @param type
	 *            The type object used to register. The type must match the (field)
	 *            type of injection.
	 * @param value
	 *            The value to inject.
	 */
	public void registerDependencyMapping(final Class<?> type, final Object value) {
		if (valueMapping.containsKey(type)) {
			throw new IllegalArgumentException("Type already registered");
		}
		if (value != null && !type.isInstance(value)) {
			throw new IllegalArgumentException("Value doesn't match type");
		}
		if (Collection.class.isInstance(value)) {
			throw new IllegalArgumentException("Collection's are not supported for injection");
		}
		if (type.isPrimitive()) {
			throw new IllegalArgumentException("Primitive types are not supported for injection");
		}
		if (FORBIDDEN_TYPES.contains(type)) {
			throw new IllegalArgumentException("Type is not allowed for injection");
		}
		valueMapping.put(type, value);
	}

	/**
	 * Traverse the fields of given object to inject available instances as value to
	 * the fields.
	 */
	public void injectFields(final Object target) throws ODataApplicationException {
		if (target == null) {
			return;
		}
		final Collection<InjectionOccurrence> occurrences = findAnnotatedFields(target.getClass());
		for (final InjectionOccurrence o : occurrences) {
			final boolean accessible = o.field.isAccessible();
			if (!accessible) {
				o.field.setAccessible(true);
			}
			try {
				o.field.set(target, o.matchingObject);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			} finally {
				// reset
				if (!accessible) {
					o.field.setAccessible(false);
				}
			}
		}
	}

	/**
	 *
	 * @param field
	 *            The field to check
	 * @return TRUE if given field has a annotation assumed to be a injection marker
	 */
	private static boolean isAnnotatedForInjection(final Field field) {
		//olingo-jpa-processor specific 'Inject' annotation
		if(field.isAnnotationPresent(Inject.class)) {
			return true;
		}
		//support for javax.inject.Inject, avoiding direct dependencies
		if (injectAnnotation != null && field.isAnnotationPresent(injectAnnotation)) {
			return true;
		}
		return false;
	}

	private Collection<InjectionOccurrence> findAnnotatedFields(final Class<?> clazz) {
		if (Object.class.equals(clazz)) {
			// don't inspect Object class
			return Collections.emptyList();
		}
		final Field[] clazzFields = clazz.getDeclaredFields();
		final Collection<InjectionOccurrence> occurrences = new LinkedList<>();
		Object value;
		for (final Field field : clazzFields) {
			if (isAnnotatedForInjection(field)) {
				value = findMatchingValue(field);
				if (value != null) {
					occurrences.add(new InjectionOccurrence(field, value));
				}
			}
		}
		final Class<?> clazzSuper = clazz.getSuperclass();
		if (clazzSuper != null) {
			final Collection<InjectionOccurrence> superOccurrences = findAnnotatedFields(clazzSuper);
			occurrences.addAll(superOccurrences);
		}
		return occurrences;
	}

	private Object findMatchingValue(final Field field) {
		for (final Entry<Class<?>, Object> entry : valueMapping.entrySet()) {
			if (isMatchingType(entry.getKey(), field.getType())) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 *
	 * @param requestedType
	 *            The type from {@link #registerDependencyMapping(Class, Object)}
	 * @param actualType
	 *            The type of {@link Field#getType()} or
	 *            {@link java.lang.reflect.Parameter#getType()}
	 * @return TRUE if types are matching
	 */
	private boolean isMatchingType(final Class<?> requestedType, final Class<?> actualType) {
		return requestedType.isAssignableFrom(actualType);
	}
}
