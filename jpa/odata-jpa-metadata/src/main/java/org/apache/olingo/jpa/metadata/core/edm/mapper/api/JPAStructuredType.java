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
   * On Java side an open type is supported via {@link java.util.Map}. An open type must also implement
   * {@link JPADynamicPropertyContainer}.
   *
   * @return TRUE if instances of that type can have properties, not declared as attribute.
   * @see org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateMapComplexTypeDTO
   */
  public boolean isOpenType();

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
   * <li>All not ignored navigation properties from super types are included
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

  /**
   * Returns an attribute regardless if it should be ignored or not
   *
   * @return The attribute or <code>null</code>
   */
  public JPAMemberAttribute getAttribute(String internalName) throws ODataJPAModelException;

  /**
   * <i>exploded</i> should be TRUE to represent the OData model view and FALSE for internal processing.
   *
   * @return All not {@link JPAAttribute#ignore() ignored} attributes, including attributes from base type.
   * @see {@link #getAllAttributes(boolean)}
   */
  public List<JPAMemberAttribute> getAttributes(boolean exploded) throws ODataJPAModelException;

  /**
   * Returns a resolved list of all attributes. If <i>exploded</i> is TRUE then the attributes of an @EmbeddedId
   * property (or other complex attribute types) are listed all as separate entries, if FALSE a @EmbeddedId is returned
   * as one entry, the nested key attributes are not separated.
   *
   * @return A list including also {@link JPAMemberAttribute#ignore() ignored} attributes and also all attributes from
   * {@link #getBaseType() base type}.
   */
  public List<JPAMemberAttribute> getAllAttributes(boolean exploded) throws ODataJPAModelException;

  /**
   *
   * @return All not {@link JPAAttribute#ignore() ignored} relationships.
   */
  public List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException;

  /**
   * Resolve simple, complex and association attribute DB paths.
   */
  public JPASelector getPath(String externalName) throws ODataJPAModelException;

  /**
   * List of all simple (and not complex) attributes that are available for this type via the OData
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
   * Returns a resolved list of all attributes that are marked as Id.
   *
   * @return The list with attributes or empty list.
   *
   * @throws ODataJPAModelException
   * @see {@link #getAllAttributes(boolean)}
   */
  public List<JPAMemberAttribute> getKeyAttributes(boolean exploded) throws ODataJPAModelException;

  /**
   * All searchable attributes from {@link #getPathList()}.
   * @throws ODataJPAModelException
   */
  public List<JPASelector> getSearchablePath() throws ODataJPAModelException;

}
