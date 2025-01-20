package org.apache.olingo.jpa.processor.transformation;

import java.util.List;

import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;

import jakarta.validation.constraints.NotNull;

/**
 * A chain defines a sequence of one or more transformations.
 */
public interface TransformationChain<Input, Output> extends Transformation<Input, Output> {
	/**
	 * @return List of chain elements.
	 */
	public @NotNull List<Transformation<?,?>> getSteps();
	
	/**
	 * Fluent API to build type safe chains.
	 * 
	 * @param <X> The new <i>Output</i> of {@linkplain Transformation}
	 * @param chainedElement The element to add to tail of chain.
	 * @return The type modified transformation.
	 */
	public <X> @NotNull TransformationChain<Input, X> addStep(@NotNull Transformation<Output, X> chainedElement);
	
	/**
	 * Find a sub chain providing the transformation <i>from</i> X <i>to</i> Y or throw an exception.
	 */
	public <X, Y> TransformationChain<X, Y> createSubTransformation(@NotNull Class<X> from, @NotNull Class<Y> to) throws ODataJPAConversionException;
	
}
