package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * External view on an Intermediate Structured Type.
 *
 * @author Oliver Grande
 *
 */
public interface JPAStructuredType extends JPAElement {

  public boolean isAbstract();

  /**
   *
   * @return TRUE if element should be ignored.
   */
  public boolean ignore();

  /**
   * Searches in the navigation properties that are available for this type via
   * the OData service. That is:
   * <ul>
   * <li>All not ignored navigation properties of this type.
   * <li>All not ignored navigation properties from supertypes are included
   * <li>All not ignored navigation properties from embedded types are included.
   * </ul>
   *
   * @return null if no navigation property found.
   * @throws ODataJPAModelException
   */
  public List<JPAAssociationPath> getAssociationPathList() throws ODataJPAModelException;

  public JPAAssociationAttribute getAssociationByPath(final JPAAssociationPath path) throws ODataJPAModelException;

  public JPAAssociationPath getAssociationPath(String externalName) throws ODataJPAModelException;

  public JPAAssociationPath getDeclaredAssociation(String externalName) throws ODataJPAModelException;

  public JPASimpleAttribute getAttribute(String internalName) throws ODataJPAModelException;

  public List<JPASimpleAttribute> getAttributes() throws ODataJPAModelException;

  public List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException;

  /**
   * Resolve simple, complex and association attribute DB paths.
   */
  public JPASelector getPath(String externalName) throws ODataJPAModelException;

  /**
   * List of all attributes that are available for this type via the OData
   * service. That is:
   * <ul>
   * <li>All not ignored properties of the type.
   * <li>All not ignored properties from supertypes.
   * <li>All not ignored properties from embedded types.
   * </ul>
   *
   * @return List of all attributes that are available via the OData service.
   * @throws ODataJPAModelException
   * @see {@link org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore @EdmIgnore}
   */
  public List<JPASelector> getPathList() throws ODataJPAModelException;

  public Class<?> getTypeClass();

  /**
   * In case the type is within the given association path, the sub-path is
   * returned. E.g. structured type is AdministrativeInformation and
   * associationPath = AdministrativeInformation/Created/User Created/User is
   * returned.
   *
   * @param associationPath
   * @return
   * @throws ODataJPAModelException
   * @Deprecated Useless method?
   */
  @Deprecated
  public JPAAssociationPath getDeclaredAssociation(JPAAssociationPath associationPath) throws ODataJPAModelException;

  /**
   * Returns a resolved list of all attributes that are marked as Id. If
   * <i>exploded</i> is TRUE then the attributes of an @EmbeddedId property (or
   * other complex attribute types) are listed all as separate entries, if FALSE
   * a @EmbeddedId is returned as one entry, the nested key attributes are not
   * separated.
   *
   * @return The list with attributes or empty list.
   *
   * @throws ODataJPAModelException
   */
  public List<JPASimpleAttribute> getKeyAttributes(boolean exploded) throws ODataJPAModelException;

  /**
   *
   * @throws ODataJPAModelException
   */
  public List<JPASelector> getSearchablePath() throws ODataJPAModelException;

}
