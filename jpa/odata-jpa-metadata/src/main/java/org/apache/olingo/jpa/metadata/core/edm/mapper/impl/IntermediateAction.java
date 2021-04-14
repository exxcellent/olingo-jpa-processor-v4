package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Mapper, that is able to convert different metadata resources into a edm
 * action metadata.
 *
 * @author Ralf Zozmann
 *
 */
public class IntermediateAction extends IntermediateModelElement<CsdlAction> implements JPAAction {

  private CsdlAction edmAction = null;
  private final IntermediateServiceDocument isd;
  private final Class<?> javaOwnerEntityClass;
  private final Method javaMethod;
  private final List<ActionParameter> parameterList;
  private final ActionResultParameter resultParameter;
  boolean isBound = true;

  IntermediateAction(final JPAEdmNameBuilder nameBuilder, final Class<?> jpaEntityClass, final Method actionMethod,
      final IntermediateServiceDocument isd)
          throws ODataJPAModelException, IllegalArgumentException {
    super(nameBuilder, buildActionInternalName(jpaEntityClass, actionMethod));
    this.javaMethod = actionMethod;
    this.javaOwnerEntityClass = jpaEntityClass;
    if (Modifier.isStatic(actionMethod.getModifiers())) {
      if (!Modifier.isPublic(actionMethod.getModifiers())) {
        throw new IllegalArgumentException(
            "Given JAVA method must be 'public static' to be handled as unbound edm:Action");
      }
      isBound = false;
    }
    final EdmAction jpaAction = actionMethod.getAnnotation(EdmAction.class);
    if (jpaAction == null) {
      throw new IllegalArgumentException("Given JAVA method must be annotated with @"
          + EdmAction.class.getSimpleName() + " to be handled as edm:Action");
    }
    String name = jpaAction.name();
    if (name == null || name.isEmpty()) {
      name = actionMethod.getName();
    }
    this.setExternalName(name);
    this.isd = isd;

    parameterList = new ArrayList<ActionParameter>(javaMethod.getParameters().length);
    int index = 0;
    for (final Parameter p : javaMethod.getParameters()) {
      final ActionParameter ap = new ActionParameter(this, p, index);
      parameterList.add(ap);
      index++;
    }

    if (javaMethod.getReturnType() == void.class || javaMethod.getReturnType() == Void.class) {
      resultParameter = null;
    } else {
      resultParameter = new ActionResultParameter(this);
    }
  }

  IntermediateServiceDocument getIntermediateServiceDocument() {
    return isd;
  }

  /**
   * The method declaring class may differ from the real owning entity class,
   * because overload of methods is possible.
   */
  private static String buildActionInternalName(final Class<?> jpaEntityClass, final Method actionMethod) {
    return jpaEntityClass.getSimpleName().concat("::").concat(actionMethod.getName());
  }

  public Method getJavaMethod() {
    return javaMethod;
  }

  @Override
  public List<JPAOperationParameter> getParameters() {
    return Collections.unmodifiableList(parameterList);
  }

  @Override
  public JPAOperationResultParameter getResultParameter() {
    return resultParameter;
  }

  @Override
  CsdlAction getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmAction;
  }

  /**
   *
   * @return The list of parameters or <code>null</code> if empty.
   */
  private List<CsdlParameter> buildParameterList() throws ODataJPAModelException {
    final List<CsdlParameter> parameters = new LinkedList<>();
    if (isBound) {
      // if an action is 'bound' then the first parameter in list must be the entity
      // type where the action is bound to; we generate that on demand
      final FullQualifiedName fqn = isd.getStructuredType(javaOwnerEntityClass).getExternalFQN();
      final CsdlParameter parameter = new CsdlParameter();
      parameter.setName(BOUND_ACTION_ENTITY_PARAMETER_NAME);
      parameter.setNullable(false);
      parameter.setCollection(false);
      parameter.setType(fqn);
      parameters.add(parameter);
    }
    // other relevant method parameters...
    for (final ActionParameter ap : parameterList) {
      final CsdlParameter csdlParam = ap.getEdmItem();
      if (csdlParam == null) {
        continue;
      }
      parameters.add(csdlParam);
    }
    return parameters;
  }


  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmAction != null) {
      return;
    }
    edmAction = new CsdlAction();
    edmAction.setName(getExternalName());
    edmAction.setParameters(buildParameterList());
    if (resultParameter != null) {
      edmAction.setReturnType(resultParameter.getEdmItem());
    }
    edmAction.setBound(isBound);
  }

  @Override
  public boolean isBound() {
    return isBound;
  }

}
