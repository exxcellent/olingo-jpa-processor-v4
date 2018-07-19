package org.apache.olingo.jpa.processor.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Helper class to realize a limited support for dependency injection. Supported
 * are:
 * <ul>
 * <li>javax.inject.Inject: for fields</li>
 * </ul>
 *
 * @author Ralf Zozmann
 *
 */
public final class DependencyInjector {

	private static class InjectionOccurrence {
		private final Field field;
		@SuppressWarnings("unused")
		private final Annotation annotation;
		private final Object matchingObject;

		InjectionOccurrence(final Field field, final Annotation annotation,
				final Object matchingObject) {
			super();
			this.field = field;
			this.annotation = annotation;
			this.matchingObject = matchingObject;
		}
	}

	private final Map<Class<?>, Object> valueMapping = new HashMap<>();

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

	private Collection<InjectionOccurrence> findAnnotatedFields(final Class<?> clazz) {
		if (Object.class.equals(clazz)) {
			// don't inspect Object class
			return Collections.emptyList();
		}
		final Field[] clazzFields = clazz.getDeclaredFields();
		final Collection<InjectionOccurrence> occurrences = new LinkedList<>();
		Object value;
		for (final Field field : clazzFields) {
			final Inject inject = field.getAnnotation(javax.inject.Inject.class);
			if (inject != null) {
				value = findMatchingValue(field, inject);
				if (value != null) {
					occurrences.add(new InjectionOccurrence(field, inject, value));
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

	private <T extends Annotation> Object findMatchingValue(final Field field, final T annotation) {
		for (final Entry<Class<?>, Object> entry : valueMapping.entrySet()) {
			if (entry.getKey().isAssignableFrom(field.getType())) {
				return entry.getValue();
			}
		}
		return null;
	}
}
