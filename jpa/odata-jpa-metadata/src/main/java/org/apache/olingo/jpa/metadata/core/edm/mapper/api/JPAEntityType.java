package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAEntityType extends JPAStructuredType {

	@Override
	public CsdlEntityType getEdmItem() throws ODataJPAModelException;

	/**
	 *
	 * @return Mime type of streaming content
	 * @throws ODataJPAModelException
	 */
	public String getContentType() throws ODataJPAModelException;

	public JPASelector getContentTypeAttributePath() throws ODataJPAModelException;

	/**
	 * @see org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateStructuredType#getKeyAttributes()
	 */
	@Override
	public List<JPASimpleAttribute> getKeyAttributes() throws ODataJPAModelException;

	/**
	 * Returns a list of path of all attributes annotated as Id. EmbeddedId are <b>not</b> resolved
	 * @return
	 * @throws ODataJPAModelException
	 */
	public List<JPAAttributePath> getKeyPath() throws ODataJPAModelException;

	/**
	 * Returns the class of the Key. This could by either a primitive tape, the IdClass or the Embeddable of an EmbeddedId
	 * @return
	 */
	public Class<?> getKeyType();

	/**
	 *
	 * @return
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
}
