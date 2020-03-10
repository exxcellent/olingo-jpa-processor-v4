package org.apache.olingo.jpa.processor.impl;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.conversion.Transformation;
import org.apache.olingo.jpa.processor.conversion.TransformationRequest;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.EntityConverter;
import org.apache.olingo.jpa.processor.core.query.EntityCountQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.EntityQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.JPAInstanceResultConverter;
import org.apache.olingo.jpa.processor.core.query.NavigationRoot;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeCollection;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeComplex;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeEntity;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializePrimitive;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeValue;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializer;
import org.apache.olingo.jpa.processor.core.util.DTOEntityHelper;
import org.apache.olingo.jpa.processor.core.util.DependencyMapping;
import org.apache.olingo.jpa.processor.core.util.JPAEntityHelper;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ComplexProcessor;
import org.apache.olingo.server.api.processor.CountEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.FixedFormatSerializer;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.queryoption.CountOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.ExpandItemImpl;
import org.apache.olingo.server.core.uri.queryoption.ExpandOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.LevelsOptionImpl;

public class JPAStructureProcessor extends AbstractProcessor implements EntityProcessor, CountEntityCollectionProcessor,
ComplexProcessor, PrimitiveValueProcessor {

  private final Logger log = Logger.getLogger(AbstractProcessor.class.getName());

  public JPAStructureProcessor(final JPAODataSessionContextAccess context) {
    super(context);
  }

  @Override
  public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat)
          throws ODataApplicationException, ODataLibraryException {
    final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(), responseFormat,
        uriInfo);
    processSingle(request, response, uriInfo, responseFormat, serializer);
  }

  @Override
  public void createEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
    try {
      final JPAEntityType jpaEntityType = getGlobalContext().getEdmProvider().getServiceDocument()
          .getEntityType(targetEdmEntitySet.getName());
      final OData odata = getOData();
      final ServiceMetadata serviceMetadata = getServiceMetadata();
      final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());

      final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
      final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
      Entity odataEntity = deserializerResult.getEntity();

      final EntityConverter entityConverter = new EntityConverter(odata.createUriHelper(), sd, serviceMetadata);
      final Object persistenceJPAEntity = entityConverter.convertOData2JPAEntity(odataEntity, jpaEntityType);

      final DTOEntityHelper dtoHelper = new DTOEntityHelper(getGlobalContext(), uriInfo);
      if (dtoHelper.isTargetingDTOWithHandler(targetEdmEntitySet)) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_CREATE,
            HttpStatusCode.BAD_REQUEST);
      }

      final EntityManager em = getEntityManager();
      em.persist(persistenceJPAEntity);
      // force writing to DB...
      log.log(Level.FINER, "Flush new created entity of type " + jpaEntityType.getInternalName() + " to DB...");
      em.flush();
      // ...so as to reload with data from DB, also filling values not given with 'create request', but derived from DB
      log.log(Level.FINER, "Reload new created entity of type " + jpaEntityType.getInternalName()
      + " from DB to get also dervied values not given in creation request...");
      em.refresh(persistenceJPAEntity);
      // convert reverse to get also generated fields
      odataEntity = entityConverter.convertJPA2ODataEntity(jpaEntityType, persistenceJPAEntity);

      response.setHeader("Location", request.getRawBaseUri() + "/" + odataEntity.getId().toASCIIString()); // set always
      if (hasPreference(request, "return", "minimal")) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        request.setHeader(HttpHeader.ODATA_ENTITY_ID, odataEntity.getId().toASCIIString());
      } else {
        // full response containing complete entity content
        // assuming the complete action result should be expanded and serialized back to client
        ExpandOption resultExpand;
        if (uriInfo.getExpandOption() == null) {
          resultExpand = new ExpandOptionImpl();
          final ExpandItemImpl expand = new ExpandItemImpl();
          expand.setIsStar(true);
          expand.setSystemQueryOption(new LevelsOptionImpl().setMax());
          ((ExpandOptionImpl) resultExpand).addExpandItem(expand);
        } else {
          resultExpand = uriInfo.getExpandOption();
        }
        final UriHelper uriHelper = odata.createUriHelper();
        final EdmEntityType type = targetEdmEntitySet.getEntityType();
        final String selectList = uriHelper.buildContextURLSelectList(type, resultExpand, uriInfo.getSelectOption());
        final ContextURL contextUrl = ContextURL.with().entitySet(targetEdmEntitySet).suffix(Suffix.ENTITY).selectList(
            selectList).build();
        final ODataSerializer serializer = odata.createSerializer(responseFormat);
        final EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).select(uriInfo
            .getSelectOption()).expand(resultExpand).build();
        final SerializerResult serializerResult = serializer.entity(getServiceMetadata(), type, odataEntity, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }

  }

  @Override
  public void updateEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

    final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
    final OData odata = getOData();
    final ServiceMetadata serviceMetadata = getServiceMetadata();

    // DTO?
    final DTOEntityHelper helper = new DTOEntityHelper(getGlobalContext(), uriInfo);
    if (helper.isTargetingDTOWithHandler(targetEdmEntitySet)) {
      try {
        final JPAEntityType jpaEntityType = getGlobalContext().getEdmProvider().getServiceDocument()
            .getEntityType(targetEdmEntitySet.getName());
        final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());
        final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
        final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
        final Entity odataEntity = deserializerResult.getEntity();

        helper.updateEntity(targetEdmEntitySet, odataEntity);

        // full response containing complete entity content
        final EntityCollection entityCollectionResult = new EntityCollection();
        entityCollectionResult.getEntities().add(odataEntity);
        final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(), responseFormat,
            uriInfo);
        // serialize the first (and only) entry
        final SerializerResult serializerResult = serializer.serialize(request, entityCollectionResult);
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, requestFormat.toContentTypeString());
        return;
      } catch (final ODataJPAModelException e) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }

    // normal JPA entity handling
    final TransformationRequest<QueryEntityResult, EntityCollection> descriptor = new TransformationRequest<>(
        QueryEntityResult.class, EntityCollection.class);
    final Transformation<QueryEntityResult, EntityCollection> transformation = getRequestContext()
        .getTransformerFactory()
        .createTransformation(descriptor, new DependencyMapping(UriInfoResource.class, uriInfo));
    final EntityCollection entityCollectionCompleteEntities = retrieveEntityData(request, uriInfo, transformation);

    if (entityCollectionCompleteEntities.getEntities() == null || entityCollectionCompleteEntities.getEntities().isEmpty()) {
      response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
    } else if (entityCollectionCompleteEntities.getEntities().size() > 1) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR);
    } else {
      try {
        final JPAEntityType jpaEntityType = getGlobalContext().getEdmProvider().getServiceDocument()
            .getEntityType(targetEdmEntitySet.getName());
        final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());

        final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
        final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
        // if PATCH method, then only a few properties are set (and no ID...)
        final Entity odataEntityPatchData = deserializerResult.getEntity();
        final Entity odataEntityMerged = mergeEntities(odataEntityPatchData,
            entityCollectionCompleteEntities.getEntities().get(0));

        final EntityManager em = getEntityManager();
        final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, uriInfo, odata.createUriHelper(),
            getGlobalContext());
        // load the entity as JPA instance from DB, using the ID from resource path
        final Object persistenceEntity = invoker.lookupJPAEntity(jpaEntityType, odataEntityMerged);
        if (persistenceEntity == null) {
          throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
              HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        final EntityConverter entityConverter = new EntityConverter(odata.createUriHelper(), sd, serviceMetadata);
        final Object persistenceModifiedEntity = entityConverter.convertOData2JPAEntity(odataEntityMerged,
            jpaEntityType);
        // FIXME we cannot use em.merge(), because relationships are removed...
        final Object persistenceMergedEntity = em.merge(persistenceModifiedEntity);

        // convert reverse to get also generated fields
        final Entity odataEntityUpdated = entityConverter.convertJPA2ODataEntity(jpaEntityType,
            persistenceMergedEntity);

        // full response containing complete entity content
        final EntityCollection entityCollectionResult = new EntityCollection();
        entityCollectionResult.getEntities().add(odataEntityUpdated);
        final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(),
            responseFormat, uriInfo);
        // serialize the first (and only) entry
        final SerializerResult serializerResult = serializer.serialize(request, entityCollectionResult);
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
      } catch (final ODataException e) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }
  }

  @Override
  public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {

    final TransformationRequest<QueryEntityResult, EntityCollection> descriptor = new TransformationRequest<>(
        QueryEntityResult.class, EntityCollection.class);
    final Transformation<QueryEntityResult, EntityCollection> transformation = getRequestContext()
        .getTransformerFactory()
        .createTransformation(descriptor, new DependencyMapping(UriInfoResource.class, uriInfo));
    final EntityCollection entityCollection = retrieveEntityData(request, uriInfo, transformation);

    if (entityCollection.getEntities() == null || entityCollection.getEntities().isEmpty()) {
      // a 'dummy' message content will prevent the OData client reponse parser from
      // exceptions because empty body
      response.setContent(new ByteArrayInputStream("{}".getBytes()));
      response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
    } else {
      final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
      final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
      try {
        final EntityManager em = getEntityManager();
        final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, uriInfo, getOData()
            .createUriHelper(),
            getGlobalContext());
        final JPAEntityType jpaType = sd.getEntityType(targetEdmEntitySet.getName());
        for (final Entity entity : entityCollection.getEntities()) {
          final Object persistenceEntity = invoker.lookupJPAEntity(jpaType, entity);
          em.remove(persistenceEntity);
        }
        // ok
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      } catch (final ODataException e) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }

  }

  private EntityCollection retrieveFunctionData(final ODataRequest request, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {

    final UriResourceFunction uriResourceFunction = (UriResourceFunction) uriInfo.getUriResourceParts().get(0);
    final JPAFunction jpaFunction = sd.getFunction(uriResourceFunction.getFunction());
    final JPAOperationResultParameter resultParameter = jpaFunction.getResultParameter();
    if (resultParameter.isPrimitive()) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
          HttpStatusCode.NOT_IMPLEMENTED, jpaFunction.getResultParameter().getTypeFQN()
          .getFullQualifiedNameAsString());
    }
    final JPAEntityType returnType = sd.getEntityType(jpaFunction.getResultParameter().getTypeFQN());
    if (returnType == null) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
          HttpStatusCode.INTERNAL_SERVER_ERROR, jpaFunction.getResultParameter().getTypeFQN().getFullQualifiedNameAsString());
    }

    // dbProcessor.query
    final JPAODataDatabaseProcessor dbProcessor = getGlobalContext().getDatabaseProcessor();
    final List<?> nr = dbProcessor.executeFunctionQuery(uriResourceFunction, jpaFunction, returnType,
        getEntityManager());

    final EdmEntitySet returnEntitySet = uriResourceFunction.getFunctionImport().getReturnedEntitySet();
    try {
      final JPAInstanceResultConverter converter = new JPAInstanceResultConverter(getOData().createUriHelper(),
          sd, nr, returnEntitySet, returnType.getTypeClass());
      return converter.getResult();
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    } catch (final URISyntaxException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_URI_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }

  }

  /**
   * Central method to load (entity/dto) data from a source.
   */
  @SuppressWarnings("unchecked")
  private <O> O retrieveEntityData(final ODataRequest request, final UriInfo uriInfo,
      final Transformation<QueryEntityResult, O> transformation)
          throws ODataApplicationException, ODataLibraryException {

    final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    final int lastPathSegmentIndex = resourceParts.size() - 1;
    final UriResource lastPathSegment = resourceParts.get(lastPathSegmentIndex);
    if (lastPathSegment.getKind() == UriResourceKind.function) {
      // entity dispatching is also called for functions (but not actions)
      if (!EntityCollection.class.isAssignableFrom(transformation.getOutputType())) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
            HttpStatusCode.BAD_REQUEST, new IllegalStateException("Transformation cannot be used for functions"));
      }
      return (O) retrieveFunctionData(request, uriInfo);
    }

    // continue with normal entity (collection) query
    final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);

    if (targetEdmEntitySet == null) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.BAD_REQUEST, new IllegalArgumentException("EntitySet not found"));
    }

    final ServiceMetadata serviceMetadata = getServiceMetadata();
    final DTOEntityHelper helper = new DTOEntityHelper(getGlobalContext(), uriInfo);
    if (helper.isTargetingDTOWithHandler(targetEdmEntitySet)) {
      if (!EntityCollection.class.isAssignableFrom(transformation.getOutputType())) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
            HttpStatusCode.BAD_REQUEST, new IllegalStateException("Transformation cannot be used for DTO's"));
      }
      return (O) helper.loadEntities(targetEdmEntitySet);
    } else {
      // Create a JPQL Query and execute it
      try {
        // load entities
        final EntityQueryBuilder query = new EntityQueryBuilder(getGlobalContext(), new NavigationRoot(uriInfo),
            getEntityManager(),
            serviceMetadata);
        return query.execute(true, transformation);

      } catch (final ODataJPAModelException e) {
        throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }
  }

  @Override
  public void readEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

    final TransformationRequest<QueryEntityResult, EntityCollection> descriptor = new TransformationRequest<>(
        QueryEntityResult.class, EntityCollection.class/*
         * , RepresentationType.COLLECTION_ENTITY,
         * new ContentTypeComparable(responseFormat)
         */);
    final Transformation<QueryEntityResult, EntityCollection> transformation = getRequestContext()
        .getTransformerFactory()
        .createTransformation(descriptor, new DependencyMapping(UriInfoResource.class, uriInfo));

    final EntityCollection entityCollection = retrieveEntityData(request, uriInfo, transformation);
    if (entityCollection.getEntities() == null) {
      // 404 Not Found indicates that the resource specified by the request URL does
      // not exist. The response body MAY
      // provide additional information.
      // A request returns 204 No Content if the requested resource has the null
      // value, or if the service applies a
      // return=minimal preference. In this case, the response body MUST be empty.
      // Assumption 404 is handled by Olingo during URL parsing
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    } else {
      final JPASerializer serializer = new JPASerializeCollection(getServiceMetadata(), getOData(),
          responseFormat, uriInfo);
      // serialize all entries
      final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
      response.setContent(serializerResult.getContent());
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
  }

  @Override
  public void countEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    // enforce $count option as given, because OLingo parser doesn't respect the
    // last resource path as system query option (a bug in Olingo?!)
    if (uriInfo.getCountOption() == null) {
      log.log(Level.FINER, "Add count option to UriInfo, because Olingo has not set it");
      final CountOptionImpl countOption = new CountOptionImpl();
      countOption.setValue(true);
      ((UriInfoImpl) uriInfo).setSystemQueryOption(countOption);
    }

    // count entities
    try {
      final EntityCountQueryBuilder query = new EntityCountQueryBuilder(getGlobalContext(), new NavigationRoot(uriInfo),
          getEntityManager());
      final long count = query.execute();
      final FixedFormatSerializer serializer = getOData().createFixedFormatSerializer();
      response.setContent(serializer.count(Integer.valueOf(Long.valueOf(count).intValue())));
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  public void deleteComplex(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }

  @Override
  public void updateComplex(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_UPDATE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }

  private void processSingle(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat, final JPASerializer serializer)
          throws ODataApplicationException, ODataLibraryException {
    final TransformationRequest<QueryEntityResult, EntityCollection> descriptor = new TransformationRequest<>(
        QueryEntityResult.class, EntityCollection.class);
    final Transformation<QueryEntityResult, EntityCollection> transformation = getRequestContext()
        .getTransformerFactory()
        .createTransformation(descriptor, new DependencyMapping(UriInfoResource.class, uriInfo));
    final EntityCollection entityCollection = retrieveEntityData(request, uriInfo, transformation);
    if (entityCollection.getEntities() == null || entityCollection.getEntities().isEmpty()) {
      // 404 Not Found indicates that the resource specified by the request URL does
      // not exist. The response body MAY
      // provide additional information.
      // A request returns 204 No Content if the requested resource has the null
      // value, or if the service applies a
      // return=minimal preference. In this case, the response body MUST be empty.
      // Assumption 404 is handled by Olingo during URL parsing
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    } else if (entityCollection.getEntities().size() > 1) {
      throw new ODataApplicationException("More than one entity found for request",
          HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
    } else {
      // serialize the first (and only) entry
      final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
      response.setContent(serializerResult.getContent());
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
  }

  @Override
  public void readComplex(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat)
          throws ODataApplicationException, ODataLibraryException {
    final JPASerializer serializer = new JPASerializeComplex(getServiceMetadata(), getOData().createSerializer(
        responseFormat), getOData().createUriHelper(), uriInfo);
    processSingle(request, response, uriInfo, responseFormat, serializer);
  }

  @Override
  public void readPrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat)
          throws ODataApplicationException, ODataLibraryException {
    final JPASerializer serializer = new JPASerializePrimitive(getServiceMetadata(), getOData().createSerializer(
        responseFormat), getOData().createUriHelper(), uriInfo);
    processSingle(request, response, uriInfo, responseFormat, serializer);
  }

  @Override
  public void updatePrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_UPDATE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }

  @Override
  public void deletePrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }

  @Override
  public void readPrimitiveValue(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    final JPASerializer serializer = new JPASerializeValue(getServiceMetadata(), getOData()
        .createFixedFormatSerializer(), getOData().createUriHelper(), uriInfo);
    processSingle(request, response, uriInfo, responseFormat, serializer);
  }

  @Override
  public void updatePrimitiveValue(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat) throws ODataApplicationException,
  ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_UPDATE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }

  @Override
  public void deletePrimitiveValue(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.NOT_IMPLEMENTED);
  }
}
