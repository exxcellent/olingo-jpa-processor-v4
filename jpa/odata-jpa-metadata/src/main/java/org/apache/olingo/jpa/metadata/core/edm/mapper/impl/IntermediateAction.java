package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
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
   * Helper method to extract 'parameter type' from a parameterized (generic) type like a {@link Collection}.
   */
  FullQualifiedName extractGenericTypeQualifiedName(final Type type, final String elementName)
      throws ODataJPAModelException {
    if (ParameterizedType.class.isInstance(type) && Class.class.isInstance(ParameterizedType.class.cast(type)
        .getRawType()) && Map.class.isAssignableFrom(Class.class.cast(ParameterizedType.class.cast(type)
            .getRawType()))) {
      // special handling for java.util.Map (not handled as parameterized type)
      final Triple<Class<?>, Class<?>, Boolean> typeInfo = checkMapTypeArgumentsMustBeSimple(type, elementName);
      final AbstractIntermediateComplexTypeDTO jpaMapType = isd.createDynamicJavaUtilMapType(typeInfo
          .getLeft(), typeInfo.getMiddle(), typeInfo.getRight().booleanValue());
      return jpaMapType.getExternalFQN();
    }

    final Class<?> clazzType = determineClassType(type);
    // now adapt to oData type to determine FQN
    final JPAStructuredType et = isd.getEntityType(clazzType);
    if (et != null) {
      if (et.ignore()) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_WRONG_TYPE);
      }
      return et.getExternalFQN();
    } else if (clazzType.isEnum()) {
      final IntermediateEnumType enT = isd.getEnumType(clazzType);
      if (enT != null) {
        return enT.getExternalFQN();
      }
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED, type.getTypeName());
    } else {
      // may throw an ODataJPAModelException
      final EdmPrimitiveTypeKind simpleType = TypeMapping.convertToEdmSimpleType(clazzType);
      return simpleType.getFullQualifiedName();
    }
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
      final FullQualifiedName fqn = extractGenericTypeQualifiedName(javaOwnerEntityClass, "binding instance");
      final CsdlParameter parameter = new CsdlParameter();
      parameter.setName(BOUND_ACTION_ENTITY_PARAMETER_NAME);
      parameter.setNullable(false);// TODO mark as 'nullable' to work with Deserializer missing the 'bound resource parameter'?
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
