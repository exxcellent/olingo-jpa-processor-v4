package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397974"
 * >OData Version 4.0 Part 3 - 8 Entity Type</a>
 * @author Oliver Grande
 *
 */
class IntermediateEnumType extends IntermediateModelElement {
	private final Class<? extends Enum<?>> enumType;
	private CsdlEnumType edmEnumType = null;

	IntermediateEnumType(final JPAEdmNameBuilder nameBuilder, final Class<? extends Enum<?>> enumType,
			final IntermediateServiceDocument serviceDocument)
					throws ODataJPAModelException {
		super(nameBuilder, enumType.getName());
		this.enumType = enumType;
		this.setExternalName(nameBuilder.buildEnumTypeName(enumType));
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmEnumType != null) {
			return;
		}
		edmEnumType = new CsdlEnumType();
		edmEnumType.setName(getExternalName());
		edmEnumType.setFlags(false); // always false
		edmEnumType.setMembers(buildEnumMembers());
		edmEnumType.setUnderlyingType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
	}

	private List<CsdlEnumMember> buildEnumMembers() throws ODataJPAModelException {
		final Enum<?>[] literals = enumType.getEnumConstants();
		if(literals == null) {
			return new LinkedList<CsdlEnumMember>();
		}
		final List<CsdlEnumMember> listMembers = new ArrayList<>(literals.length);
		CsdlEnumMember member;
		for(final Enum<?> literal: literals) {
			member = new CsdlEnumMember();
			member.setName(literal.name());
			member.setValue(Integer.toString(literal.ordinal()));
			listMembers.add(member);
		}
		return listMembers;
	}

	@Override
	CsdlEnumType getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmEnumType;
	}
}
