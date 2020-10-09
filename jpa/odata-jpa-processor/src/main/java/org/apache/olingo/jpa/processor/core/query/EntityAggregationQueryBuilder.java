package org.apache.olingo.jpa.processor.core.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.EdmEntityTypeImpl;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAEntityFilterProcessor;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent.ContentState;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.ApplyItem;
import org.apache.olingo.server.api.uri.queryoption.ApplyItem.Kind;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.apply.Aggregate;
import org.apache.olingo.server.api.uri.queryoption.apply.AggregateExpression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

public class EntityAggregationQueryBuilder extends AbstractCriteriaQueryBuilder<CriteriaQuery<Tuple>, Tuple> {

  private final CriteriaQuery<Tuple> cq;
  private final Root<?> root;
  private final List<AggregateExpression> aggregateExpressions;

  public EntityAggregationQueryBuilder(final JPAODataRequestContext context, final NavigationIfc uriInfo,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(context, uriInfo, em);
    aggregateExpressions = determineAggregateExpressions(uriInfo);
    cq = getCriteriaBuilder().createTupleQuery();
    root = cq.from(getQueryStartType().getTypeClass());
    // now we are ready
    initializeQuery();
  }

  private List<AggregateExpression> determineAggregateExpressions(final NavigationIfc uriInfo)
      throws ODataApplicationException {
    List<AggregateExpression> expressions = null;
    for (final UriInfoResource step : uriInfo.getNavigationSteps()) {
      final ApplyOption apply = step.getApplyOption();
      if (apply == null) {
        continue;
      }
      for (final ApplyItem item : apply.getApplyItems()) {
        if (item.getKind() != Kind.AGGREGATE) {
          throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
              HttpStatusCode.NOT_IMPLEMENTED, "Only aggregate() calls are supported for $apply");
        }
        if (expressions != null) {
          throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
              HttpStatusCode.NOT_IMPLEMENTED, "Found multiple aggregate() calls, only one is supported");
        }
        expressions = ((Aggregate) item).getExpressions();
        for (final AggregateExpression aggExp : expressions) {
          if (aggExp.getStandardMethod() == null) {
            throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
                HttpStatusCode.NOT_IMPLEMENTED, "Only standard emthods are supported for aggegrate()");
          }
        }
      }
    }
    if (expressions == null || expressions.isEmpty()) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.BAD_REQUEST, "No aggregate() call found");
    }
    return expressions;
  }

  @Override
  public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return cq.subquery(subqueryResultType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public From<?, ?> getQueryStartFrom() {
    return root;
  }

  private Expression<?>[] createAggregationSelect() throws ODataApplicationException {
    final List<Expression<? extends Number>> selects = new ArrayList<>(aggregateExpressions.size());
    final JPAEntityType targetType = getQueryResultType();
    final FilterQueryBuilderContext filterContext = new FilterQueryBuilderContext(targetType, getQueryResultFrom());
    final JPAODataDatabaseProcessor dbProcessor = getContext().getDatabaseProcessor();

    for (final AggregateExpression aggExpressionDefinition : aggregateExpressions) {
      final JPAEntityFilterProcessor<Number> filter = new JPAEntityFilterProcessor<>(getOData(), getServiceDocument(),
          getEntityManager(), targetType, dbProcessor, aggExpressionDefinition.getPath(),
          aggExpressionDefinition.getExpression(), filterContext);

      try {
        Expression<? extends Number> expressionFunction;
        final Expression<Number> filterExpression = filter.compile();
        switch (aggExpressionDefinition.getStandardMethod()) {
        case SUM:
          expressionFunction = getCriteriaBuilder().sum(filterExpression);
          break;
        case MAX:
          expressionFunction = getCriteriaBuilder().max(filterExpression);
          break;
        case MIN:
          expressionFunction = getCriteriaBuilder().min(filterExpression);
          break;
        case AVERAGE:
          expressionFunction = getCriteriaBuilder().avg(filterExpression);
          break;
        default:
          throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
              HttpStatusCode.NOT_IMPLEMENTED, "Standard method " + aggExpressionDefinition.getStandardMethod()
              + " is not supported for aggregate()");
        }
        if (aggExpressionDefinition.getAlias() != null && !aggExpressionDefinition.getAlias().isEmpty()) {
          expressionFunction.alias(aggExpressionDefinition.getAlias());
        } else {
          expressionFunction.alias(aggExpressionDefinition.getStandardMethod().name());
        }
        selects.add(expressionFunction);
      } catch (final ExpressionVisitException e) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
            HttpStatusCode.BAD_REQUEST, e);
      }
    }
    return selects.toArray(new Expression[selects.size()]);
  }

  public final ODataResponseContent execute(final ContentType responseFormat) throws ODataApplicationException,
  ODataJPAModelException, SerializerException {
    final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
    /* final Map<String, From<?, ?>> resultsetAffectingTables = */ createFromClause(orderByNaviAttributes);

    cq.multiselect(createAggregationSelect());

    final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }
    final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);

    final List<Tuple> intermediateResult = tq.getResultList();
    return transform(intermediateResult, responseFormat);
  }

  private ODataResponseContent transform(final List<Tuple> intermediateResult, final ContentType responseFormat)
      throws SerializerException, ODataJPAModelException {
    assert intermediateResult.size() == 1;
    // build an entity collection with one element containing all the aggregated values

    final EntityCollection odataEntityCollection = new EntityCollection();
    final Tuple row = intermediateResult.get(0);
    final Entity odataEntity = new Entity();
    final List<Property> properties = odataEntity.getProperties();

    for (final TupleElement<?> element : row.getElements()) {
      final Property p = new Property(null, element.getAlias());
      final Object result = row.get(element.getAlias());
      // if aggregation is working on an empty result set we got 'null' as result, but we want to give back at least a
      // zero...
      p.setValue(ValueType.PRIMITIVE, result != null ? result : BigDecimal.valueOf(0));
      properties.add(p);
    }

    odataEntityCollection.getEntities().add(odataEntity);
    // create on demand type to get the pseudo entity serialized to client
    final ODataSerializer serializer = getContext().getOdata().createSerializer(responseFormat);

    final EdmEntityType edmType = createDynamicEdmType(odataEntity);

    final ContextURL contextUrl = ContextURL.with().type(edmType).build();
    final EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextUrl)
        .build();
    final SerializerResult serializerResult = serializer.entityCollection(getContext().getServiceMetaData(), edmType,
        odataEntityCollection, opts);
    return new ODataResponseContent(ContentState.PRESENT, serializerResult.getContent());

  }

  private EdmEntityType createDynamicEdmType(final Entity odataEntity) throws ODataJPAModelException {
    final CsdlEntityType dT = createDynamicType(odataEntity);
    final FullQualifiedName name = new FullQualifiedName(dT.getName());
    return new EdmEntityTypeImpl(getContext().getServiceMetaData().getEdm(), name, dT);
  }

  private CsdlEntityType createDynamicType(final Entity odataEntity) throws ODataJPAModelException {
    final CsdlEntityType csdlEntityType = new CsdlEntityType();
    csdlEntityType.setName(UUID.randomUUID().toString() + "." + getQueryResultType().getExternalName() + "Aggregation");

    for (final Property odataProperty : odataEntity.getProperties()) {
      final CsdlProperty csdlProperty = new CsdlProperty();
      csdlProperty.setName(odataProperty.getName());
      final boolean valueIsNull = odataProperty.getValue() == null;
      final EdmPrimitiveTypeKind kind = valueIsNull ? EdmPrimitiveTypeKind.Decimal : TypeMapping.convertToEdmSimpleType(
          odataProperty.getValue().getClass());
      csdlProperty.setType(kind.getFullQualifiedName());
      csdlProperty.setScale(determineScale(odataProperty.getValue()));
      // don't set precision to avoid trouble with serializer for small numbers (double)
      csdlProperty.setNullable(valueIsNull);
      csdlProperty.setCollection(false);

      csdlEntityType.getProperties().add(csdlProperty);
    }
    return csdlEntityType;
  }

  private Integer determineScale(final Object value) {
    if (BigDecimal.class.isInstance(value)) {
      return Integer.valueOf(BigDecimal.class.cast(value).scale());
    }
    if (Double.class.isInstance(value)) {
      return Integer.valueOf(BigDecimal.valueOf(Double.class.cast(value).doubleValue()).scale());
    }
    return null;
  }
}
