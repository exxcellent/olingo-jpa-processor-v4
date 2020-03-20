package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
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
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.UriResourceValue;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;

public class Util {

  public static final String VALUE_RESOURCE = "$VALUE";

  private final static Logger LOG = Logger.getLogger(Util.class.getName());

  /**
   * Select the first {@link UriResourceEntitySet} in resource path.
   */
  public static UriResourceEntitySet determineStartingEntityUriResource(final UriInfoResource uriInfo) {
    if (uriInfo == null || uriInfo.getUriResourceParts() == null) {
      return null;
    }
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
    if (resources == null) {
      return null;
    }
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
      } else if (naviStart instanceof UriResourceProperty) {
        return null;
      } else {
        return sd.getEntityType(((UriResourceNavigation) naviStart).getProperty().getType())
            .getAssociationPath(associationName);
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAUtilException(ODataJPAUtilException.MessageKeys.UNKNOWN_NAVI_PROPERTY,
          HttpStatusCode.BAD_REQUEST);
    }
  }

  /**
   * $expand is taken from the {@link NavigationIfc#getLastStep() last step}.
   */
  public static Map<NavigationViaExpand, JPAAssociationPath> determineExpands(final IntermediateServiceDocument sd,
      final NavigationIfc uriResource) throws ODataApplicationException {

    final List<UriResource> startResourceList = uriResource.getUriResourceParts();
    final ExpandOption expandOption = uriResource.getLastStep().getExpandOption();
    final Map<NavigationViaExpand, JPAAssociationPath> pathList =
        new HashMap<NavigationViaExpand, JPAAssociationPath>();
    if (startResourceList == null || startResourceList.isEmpty() || expandOption == null) {
      return pathList;
    }

    final StringBuffer associationNamePrefix = new StringBuffer();

    UriResourcePartTyped startResourceItem = null;

    // Example1 : /Organizations('3')/AdministrativeInformation?$expand=Created/User
    // Example2 : /Organizations('3')/AdministrativeInformation?$expand=*
    // Association name needs AdministrativeInformation as prefix
    for (int i = startResourceList.size() - 1; i >= 0; i--) {
      final UriResource resource = startResourceList.get(i);
      if (resource instanceof UriResourceEntitySet || resource instanceof UriResourceNavigation) {
        startResourceItem = (UriResourcePartTyped) resource;
        break;
      }
      associationNamePrefix.insert(0, JPAAssociationPath.PATH_SEPERATOR);
      associationNamePrefix.insert(0, ((UriResourceProperty) resource).getProperty().getName());
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
          final JPAEntityType jpaEntityType = sd.getEntityType(edmEntitySet.getName());
          final List<JPAAssociationPath> associationPaths = jpaEntityType.getAssociationPathList();
          for (final JPAAssociationPath path : associationPaths) {
            final JPAAssociationAttribute asso = jpaEntityType.getAssociationByPath(path);
            // relevant for $expand are only direct relationships without longer paths coming from nested complex
            // types
            if (asso == null) {
              continue;
            }
            final EdmNavigationProperty navProperty = edmEntitySet.getEntityType().getNavigationProperty(asso
                .getExternalName());
            pathList.put(new NavigationViaExpand(uriResource, item, navProperty), path);
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
        pathList.put(new NavigationViaExpand(uriResource, item), determineAssoziationPath(sd,
            startResourceItem,
            associationName.toString()));
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
          if (association != null) {
            pathList.add(new JPANavigationPropertyInfo((UriResourcePartTyped) resourcePart, association));
          }
        }
        startType = navigation;
        complexTypeNavigation = null;// reset
      } else if (resourcePart instanceof UriResourceProperty) {
        final UriResourceProperty property = (UriResourceProperty) resourcePart;
        if (startType == null) {
          throw new IllegalStateException(
              "'UriResource(Complex/Primitive)Property' navigation found without start type "
                  + property.getProperty().getName());
        }
        if (property.isCollection()) {
          // handle @ElementCollection
          final JPAEntityType et = sd.getEntityType(startType.getType());
          if (et == null) {
            LOG.log(Level.SEVERE,
                "Resource path contains a (@ElementCollection?) navigation (" + property.getSegmentValue()
                + ") to an simple type. This state should not reached... a bug?!");
            break;
          }
          try {
            final JPASelector selector = et.getPath(property.getProperty().getName());
            final JPANavigationPath association = new JPAElementCollectionPathImpl(selector);
            pathList.add(new JPANavigationPropertyInfo((UriResourcePartTyped) resourcePart, association));
            startType = property;
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
          complexTypeNavigation.append(property.getProperty().getName());
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
    if (hasComplexPropertyCollectionNavigation(uriResourceParts)) {
      return true;
    }
    return hasPrimitivePropertyCollectionNavigation(uriResourceParts);
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

  /**
   * Detect a special variant of relationship: the navigation of 1:n to an primitive
   * type as target.
   *
   * @return TRUE if list of resource parts contains a navigation to an collection
   * of embedded primitive type
   * @see javax.persistence.ElementCollection
   */
  private static boolean hasPrimitivePropertyCollectionNavigation(final List<UriResource> uriResourceParts) {
    if (uriResourceParts == null) {
      return false;
    }
    for (final UriResource resourcePart : uriResourceParts) {
      if (resourcePart instanceof UriResourcePrimitiveProperty
          && ((UriResourcePrimitiveProperty) resourcePart).isCollection()) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @return The list of selectors for all key attributes (exploded!).
   * @see JPAEntityType#getKeyAttributes(boolean)
   */
  public static List<JPASelector> buildKeyPath(final JPAStructuredType jpaType) throws ODataJPAModelException {
    final List<? extends JPAAttribute<?>> jpaKeyList = jpaType.getKeyAttributes(true);
    final List<JPASelector> jpaPathList = new ArrayList<JPASelector>();
    for (final JPAAttribute<?> key : jpaKeyList) {
      final JPASelector keyPath = jpaType.getPath(key.getExternalName());
      jpaPathList.add(keyPath);
    }
    return jpaPathList;
  }
}
