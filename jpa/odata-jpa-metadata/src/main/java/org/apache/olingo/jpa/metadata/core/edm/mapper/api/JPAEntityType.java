package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAEntityType extends JPAStructuredType {

  /**
   *
   * @return Mime type of streaming content
   * @throws ODataJPAModelException
   */
  public String getContentType() throws ODataJPAModelException;

  public JPASelector getContentTypeAttributePath() throws ODataJPAModelException;

  /**
   * Returns a list of path of all attributes annotated as Id. EmbeddedId are
   * <b>not</b> resolved
   *
   * @throws ODataJPAModelException
   * @deprecated Use {@link #getKeyAttributes(boolean)} instead
   */
  @Deprecated
  public List<JPAAttributePath> getKeyPath() throws ODataJPAModelException;

  /**
   * Returns the class of the Key. This could by either a primitive tape, the
   * IdClass or the EmbeddedId of an Embeddable
   */
  public Class<?> getKeyType();

  /**
   *
   * @throws ODataJPAModelException
   */
  public List<JPASelector> getSearchablePath() throws ODataJPAModelException;

  public JPAAttributePath getStreamAttributePath() throws ODataJPAModelException;

  /**
   *
   * @return Name of the database table
   */
  public String getTableName();

  public boolean hasEtag() throws ODataJPAModelException;

  public boolean hasStream() throws ODataJPAModelException;

  public List<JPAAttributePath> searchChildPath(JPASelector selectItemPath);

  /**
   *
   * @return The data access handler or <code>null</code> if not defined.
   */
  public DataAccessConditioner<?> getDataAccessConditioner();

  /**
   *
   * @return The name of the entity set for this type. The name must be unique.
   */
  public String getEntitySetName();

}
