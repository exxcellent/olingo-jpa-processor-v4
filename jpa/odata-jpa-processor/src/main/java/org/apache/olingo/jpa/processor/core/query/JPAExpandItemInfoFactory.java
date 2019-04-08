package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;

class JPAExpandItemInfoFactory {

	/**
	 *
	 * @param sd
	 * @param startResourceList
	 * @param expandOption
	 * @param grandParentHops   Optional list (maybe <code>null</code>)
	 * @return
	 * @throws ODataApplicationException
	 */
	static List<JPAExpandItemInfo> buildExpandItemInfo(final IntermediateServiceDocument sd,
			final List<UriResource> startResourceList,
			final ExpandOption expandOption, final List<JPANavigationProptertyInfo> grandParentHops)
					throws ODataApplicationException {

		final List<JPAExpandItemInfo> itemList = new ArrayList<JPAExpandItemInfo>();

		if (startResourceList != null && expandOption != null) {
			final List<JPANavigationProptertyInfo> parentHops = determineParentHops(sd, startResourceList, grandParentHops);
			final UriResource startResourceItem = determineStartResourceItem(startResourceList);
			final Map<JPAExpandItemWrapper, JPAAssociationPath> expandPath = Util.determineAssoziations(sd, startResourceList,
					expandOption);
			for (final JPAExpandItemWrapper item : expandPath.keySet()) {
				itemList.add(new JPAExpandItemInfo(item, (UriResourcePartTyped) startResourceItem,
						expandPath.get(item), parentHops));
			}
		}
		return itemList;
	}

	private static UriResource determineStartResourceItem(final List<UriResource> startResourceList) {
		UriResource startResourceItem = null;
		for (int i = startResourceList.size() - 1; i >= 0; i--) {
			startResourceItem = startResourceList.get(i);
			if (startResourceItem instanceof UriResourceEntitySet || startResourceItem instanceof UriResourceNavigation) {
				break;
			}
		}
		return startResourceItem;
	}

	private static List<JPANavigationProptertyInfo> determineParentHops(final IntermediateServiceDocument sd,
			final List<UriResource> startResourceList, final List<JPANavigationProptertyInfo> grandParentHops)
					throws ODataApplicationException {
		List<JPANavigationProptertyInfo> parentHops = new ArrayList<JPANavigationProptertyInfo>();

		if (grandParentHops != null) {
			parentHops.addAll(grandParentHops);
			parentHops.addAll(Util.determineAssoziations(sd, startResourceList));
		} else {
			parentHops = Util.determineAssoziations(sd, startResourceList);
		}
		return parentHops;
	}
}
