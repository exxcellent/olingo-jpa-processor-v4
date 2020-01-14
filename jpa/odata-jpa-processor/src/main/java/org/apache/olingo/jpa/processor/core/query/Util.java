package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAElementCollectionPathImpl;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAUtilException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaVariable;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.UriResourceValue;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;

public class Util {

  public static final String VALUE_RESOURCE = "$VALUE";

  /**
   * Select the first {@link UriResourceEntitySet} in resource path.
   */
  public static UriResourceEntitySet determineStartingEntityUriResource(final UriInfoResource uriInfo) {
    for (final UriResource resourceItem : uriInfo.getUriResourceParts()) {
      switch (resourceItem.getKind()) {
      case entitySet:
        return (UriResourceEntitySet) resourceItem;
      default:
        return null;
      }
    }
    return null;
  }

  /**
   * Select the {@link EdmEntitySet} of the first {@link UriResourceEntitySet} in resource path to detect the entry
   * entity type.
   */
  public static EdmEntitySet determineStartingEntitySet(final UriInfoResource uriInfo) {
    final UriResourceEntitySet resourceItem = determineStartingEntityUriResource(uriInfo);
    if (resourceItem != null) {
      return resourceItem.getEntitySet();
    }
    return null;
  }

  /**
   * Find the last {@link UriResource} having a type identifying a {@link EdmEntitySet}.
   */
  public static EdmEntitySet determineTargetEntitySet(final List<UriResource> resources) {
    EdmEntitySet targetEdmEntitySet = null;
    StringBuffer naviPropertyName = new StringBuffer();

    for (final UriResource resourceItem : resources) {
      switch (resourceItem.getKind()) {
      case entitySet:
        targetEdmEntitySet = ((UriResourceEntitySet) resourceItem).getEntitySet();
        break;
      case complexProperty:
        naviPropertyName.append(((UriResourceComplexProperty) resourceItem).getProperty().getName());
        naviPropertyName.append(JPASelector.PATH_SEPERATOR);
        break;
      case navigationProperty:
        if (targetEdmEntitySet == null) {
          throw new IllegalStateException("UriResourceEntitySet as resources start required");
        }
        naviPropertyName.append(((UriResourceNavigation) resourceItem).getProperty().getName());
        final EdmBindingTarget edmBindingTarget = targetEdmEntitySet.getRelatedBindingTarget(naviPropertyName
            .toString());
        if (edmBindingTarget instanceof EdmEntitySet) {
          targetEdmEntitySet = (EdmEntitySet) edmBindingTarget;
        }
        naviPropertyName = new StringBuffer();
        break;
      case function:
        // bound functions have an entry of type 'entitySet' in resources-path, so we
        // can ignore other settings
        // unbound functions will have a function import targeting an optional entity
        // set
        final UriResourceFunction uriResourceFunction = (UriResourceFunction) resourceItem;
        if (uriResourceFunction.getFunction() != null && !uriResourceFunction.getFunction().isBound()
            && uriResourceFunction.getFunctionImport() != null) {
          targetEdmEntitySet = uriResourceFunction.getFunctionImport().getReturnedEntitySet();
        }
        break;
      case action:
        // bound actions have an entry of type 'entitySet' in resources-path, so we
        // can ignore other settings
        // unbound actions will have a action import targeting an optional entity set
        final UriResourceAction uriResourceAction = (UriResourceAction) resourceItem;
        if (uriResourceAction.getAction() != null && !uriResourceAction.getAction().isBound()
            && uriResourceAction.getActionImport() != null) {
          targetEdmEntitySet = uriResourceAction.getActionImport().getReturnedEntitySet();
        }
        break;
      default:
        // do nothing
        break;
      }
    }
    return targetEdmEntitySet;
  }

  /**
   * Finds an entity type from last navigation property
   */
  public static EdmEntityType determineTargetEntityType(final List<UriResource> resources) {
    EdmEntityType targetEdmEntity = null;

    for (final UriResource resourceItem : resources) {
      if (resourceItem.getKind() == UriResourceKind.navigationProperty) {
        // first try the simple way like in the example
        targetEdmEntity = (EdmEntityType) ((UriResourceNavigation) resourceItem).getType();
      }
    }
    return targetEdmEntity;
  }

  /**
   * Finds an entity type with which a navigation may starts. Can be used e.g. for filter:
   * AdministrativeDivisions?$filter=Parent/CodeID eq 'NUTS1' returns AdministrativeDivision;
   * AdministrativeDivisions(...)/Parent?$filter=Parent/CodeID eq 'NUTS1' returns "Parent"
   * @deprecated Seems not to work properly
   */
  @Deprecated
  public static EdmEntityType determineStartEntityType(final List<UriResource> resources) {
    EdmEntityType targetEdmEntity = null;

    for (final UriResource resourceItem : resources) {
      if (resourceItem.getKind() == UriResourceKind.navigationProperty) {
        // first try the simple way like in the example
        targetEdmEntity = (EdmEntityType) ((UriResourceNavigation) resourceItem).getType();
      }
      if (resourceItem.getKind() == UriResourceKind.entitySet) {
        // first try the simple way like in the example
        targetEdmEntity = ((UriResourceEntitySet) resourceItem).getEntityType();
      }
    }
    return targetEdmEntity;
  }

  /**
   * Used for Serializer
   */
  public static UriResourceProperty determineStartNavigationPath(final List<UriResource> resources) {
    UriResourceProperty property = null;
    if (resources != null) {
      for (int i = resources.size() - 1; i >= 0; i--) {
        final UriResource resourceItem = resources.get(i);
        if (resourceItem instanceof UriResourceEntitySet || resourceItem instanceof UriResourceNavigation) {
          break;
        }
        property = (UriResourceProperty) resourceItem;
      }
    }
    return property;
  }

  public static String determinePropertyNavigationPath(final List<UriResource> resources) {
    final StringBuffer pathName = new StringBuffer();
    if (resources != null) {
      for (int i = resources.size() - 1; i >= 0; i--) {
        final UriResource resourceItem = resources.get(i);
        if (resourceItem instanceof UriResourceEntitySet || resourceItem instanceof UriResourceNavigation
            || resourceItem instanceof UriResourceLambdaVariable) {
          break;
        }
        if (resourceItem instanceof UriResourceValue) {
          pathName.insert(0, VALUE_RESOURCE);
          pathName.insert(0, JPASelector.PATH_SEPERATOR);
        } else if (resourceItem instanceof UriResourceProperty) {
          final UriResourceProperty property = (UriResourceProperty) resourceItem;
          pathName.insert(0, property.getProperty().getName());
          pathName.insert(0, JPASelector.PATH_SEPERATOR);
        }
      }
      if (pathName.length() > 0) {
        pathName.deleteCharAt(0);
      }
    }
    return pathName.toString();
  }

  private static JPAAssociationPath determineAssoziationPath(final IntermediateServiceDocument sd,
      final UriResourcePartTyped naviStart,
      final String associationName) throws ODataApplicationException {

    try {
      if (naviStart instanceof UriResourceEntitySet) {
        return sd.getEntityType(naviStart.getType()).getAssociationPath(associationName);
      } else {
        return sd.getEntityType(((UriResourceNavigation) naviStart).getProperty().getType())
            .getAssociationPath(associationName);
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAUtilException(ODataJPAUtilException.MessageKeys.UNKNOWN_NAVI_PROPERTY,
          HttpStatusCode.BAD_REQUEST);
    }
  }

  public static Map<JPAExpandItemWrapper, JPAAssociationPath> determineExpands(final IntermediateServiceDocument sd,
      final List<UriResource> startResourceList, final ExpandOption expandOption) throws ODataApplicationException {

    final Map<JPAExpandItemWrapper, JPAAssociationPath> pathList =
        new HashMap<JPAExpandItemWrapper, JPAAssociationPath>();
    final StringBuffer associationNamePrefix = new StringBuffer();

    UriResource startResourceItem = null;
    if (startResourceList != null && expandOption != null) {
      // Example1 : /Organizations('3')/AdministrativeInformation?$expand=Created/User
      // Example2 : /Organizations('3')/AdministrativeInformation?$expand=*
      // Association name needs AdministrativeInformation as prefix
      for (int i = startResourceList.size() - 1; i >= 0; i--) {
        startResourceItem = startResourceList.get(i);
        if (startResourceItem instanceof UriResourceEntitySet || startResourceItem instanceof UriResourceNavigation) {
          break;
        }
        associationNamePrefix.insert(0, JPAAssociationPath.PATH_SEPERATOR);
        associationNamePrefix.insert(0, ((UriResourceProperty) startResourceItem).getProperty().getName());
      }
      // Example1 : ?$expand=Created/User (Property/NavigationProperty)
      // Example2 : ?$expand=Parent/CodeID (NavigationProperty/Property)
      // Example3 : ?$expand=Parent,Children (NavigationProperty, NavigationProperty)
      // Example4 : ?$expand=*
      // Example4 : ?$expand=*/$ref,Parent
      StringBuffer associationName;
      for (final ExpandItem item : expandOption.getExpandItems()) {
        if (item.isStar()) {
          final EdmEntitySet edmEntitySet = determineTargetEntitySet(startResourceList);
          try {
            final JPAEntityType jpaEntityType = sd.getEntitySetType(edmEntitySet.getName());
            final List<JPAAssociationPath> associationPaths = jpaEntityType.getAssociationPathList();
            for (final JPAAssociationPath path : associationPaths) {
              pathList.put(new JPAExpandItemWrapper(item, (JPAEntityType) path.getTargetType()), path);
            }
          } catch (final ODataJPAModelException e) {
            throw new ODataJPAUtilException(ODataJPAUtilException.MessageKeys.UNKNOWN_ENTITY_TYPE,
                HttpStatusCode.BAD_REQUEST);
          }
        } else {
          final List<UriResource> targetResourceList = item.getResourcePath().getUriResourceParts();
          associationName = new StringBuffer();
          associationName.append(associationNamePrefix);
          UriResource targetResourceItem = null;
          for (int i = 0; i < targetResourceList.size(); i++) {
            targetResourceItem = targetResourceList.get(i);
            if (targetResourceItem.getKind() != UriResourceKind.navigationProperty) {
              // if (i < targetResourceList.size() - 1) {
              associationName.append(((UriResourceProperty) targetResourceItem).getProperty().getName());
              associationName.append(JPAAssociationPath.PATH_SEPERATOR);
            } else {
              associationName.append(((UriResourceNavigation) targetResourceItem).getProperty().getName());
              break;
            }
          }
          pathList.put(new JPAExpandItemWrapper(sd, item), determineAssoziationPath(sd,
              ((UriResourcePartTyped) startResourceItem),
              associationName.toString()));
        }
      }
    }
    return pathList;
  }

  /**
   * Determine required navigations for associations and collections of complex
   * types (always located in another database table requiring a JOIN)
   */
  public static List<JPANavigationPropertyInfo> determineNavigations(final IntermediateServiceDocument sd,
      final List<UriResource> resourceParts) throws ODataApplicationException {

    if (!hasNavigation(resourceParts)) {
      return Collections.emptyList();
    }
    final List<JPANavigationPropertyInfo> pathList = new ArrayList<JPANavigationPropertyInfo>(resourceParts.size());

    UriResourcePartTyped startType = null;
    StringBuilder complexTypeNavigation = null;
    for (final UriResource resourcePart : resourceParts) {
      if (resourcePart instanceof UriResourceEntitySet) {
        if (startType != null) {
          throw new IllegalStateException("One more 'UriResourceEntitySet' found: "
              + ((UriResourcePartTyped) resourcePart).getType().getName());
        }
        startType = (UriResourceEntitySet) resourcePart;
        complexTypeNavigation = null;// reset
      } else if (resourcePart instanceof UriResourceNavigation) {
        final UriResourceNavigation navigation = (UriResourceNavigation) resourcePart;
        if (startType != null) {
          // startType is not given for association named in a $expand scenario
          final String assoPathName;
          if (complexTypeNavigation != null) {
            // manage previously built path
            assoPathName = complexTypeNavigation.toString().concat(JPAAssociationPath.PATH_SEPERATOR)
                .concat(navigation.getProperty().getName());
          } else {
            assoPathName = navigation.getProperty().getName();
          }
          final JPAAssociationPath association = determineAssoziationPath(sd, startType,
              assoPathName);
          pathList.add(/* 0, */
              new JPANavigationPropertyInfo((UriResourcePartTyped) resourcePart, association));
        }
        startType = navigation;
        complexTypeNavigation = null;// reset
      } else if (resourcePart instanceof UriResourceComplexProperty) {
        final UriResourceComplexProperty navigation = (UriResourceComplexProperty) resourcePart;
        if (startType == null) {
          throw new IllegalStateException("'UriResourceComplexProperty' navigation found without start type "
              + navigation.getProperty().getName());
        }
        if (navigation.isCollection()) {
          // handle @ElementCollection
          final JPAEntityType et = sd.getEntityType(startType.getType());
          try {
            final JPASelector selector = et.getPath(navigation.getProperty().getName());
            final JPANavigationPath association = new JPAElementCollectionPathImpl(selector);
            // use startType to determine entity result type, because we are used in a
            // JPAFilterQuery using 'startType' as result, because navigation is in further
            // subqueries
            pathList.add(/* 0, */ new JPANavigationPropertyInfo((UriResourcePartTyped) resourcePart, association));
            startType = navigation;
            complexTypeNavigation = null;// reset
          } catch (final ODataJPAModelException e) {
            throw new ODataJPAUtilException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
          }
        } else {
          if (complexTypeNavigation == null) {
            complexTypeNavigation = new StringBuilder();
          }
          if (complexTypeNavigation.length() > 0) {
            complexTypeNavigation.append(JPAAssociationPath.PATH_SEPERATOR);
          }
          complexTypeNavigation.append(navigation.getProperty().getName());
        }
      }
    }

    return pathList;
  }

  /**
   * @see #hasRelationshipNavigation(List)
   * @see #hasComplexPropertyCollectionNavigation(List)
   */
  public static boolean hasNavigation(final List<UriResource> uriResourceParts) {
    if (hasRelationshipNavigation(uriResourceParts)) {
      return true;
    }
    return hasComplexPropertyCollectionNavigation(uriResourceParts);
  }

  /**
   *
   * @return TRUE if list of resource parts contains a relationship (navigation)
   */
  private static boolean hasRelationshipNavigation(final List<UriResource> uriResourceParts) {
    if (uriResourceParts == null) {
      return false;
    }
    for (final UriResource resourcePart : uriResourceParts) {
      if (resourcePart instanceof UriResourceNavigation) {
        return true;
      }
    }
    return false;
  }

  /**
   * Detect a special variant of relationship: the navigation of 1:n to an complex
   * type as target.
   *
   * @return TRUE if list of resource parts contains a navigation to an collection
   *         of embedded complex type
   * @see javax.persistence.ElementCollection
   */
  private static boolean hasComplexPropertyCollectionNavigation(final List<UriResource> uriResourceParts) {
    if (uriResourceParts == null) {
      return false;
    }
    for (final UriResource resourcePart : uriResourceParts) {
      if (resourcePart instanceof UriResourceComplexProperty
          && ((UriResourceComplexProperty) resourcePart).isCollection()) {
        return true;
      }
    }
    return false;
  }
}
