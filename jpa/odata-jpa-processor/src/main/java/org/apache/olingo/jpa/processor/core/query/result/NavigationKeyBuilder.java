package org.apache.olingo.jpa.processor.core.query.result;

import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.query.Util;

/**
 * Representation of a navigation having keys for effected JOIN and to map entities loaded as $expand to owning entity.
 *
 */
public final class NavigationKeyBuilder {

  private final NavigationKeyBuilder parent;
  private final List<JPASelector> navigationKeyPaths;
  private final JPANavigationPath navigationPath;
  private final String targetLabel;
  private final JPAStructuredType jpaNavigationTargetType;
  private final int level;

  /**
   *
   * @see #NavigationKeyBuilder(NavigationKeyBuilder, JPANavigationPath, JPAStructuredType)
   */
  public NavigationKeyBuilder(final JPAStructuredType jpaType)
      throws ODataJPAModelException {
    this(null, null, jpaType);
  }

  /**
   *
   * @param parent Optional parent
   * @param navigationPath Optional
   * @param jpaNavigationTargetType The target of navigation, maybe the result table of starting FROM of an query.
   */
  private NavigationKeyBuilder(final NavigationKeyBuilder parent, final JPANavigationPath navigationPath,
      final JPAStructuredType jpaNavigationTargetType)
          throws ODataJPAModelException {
    this.parent = parent;
    if (parent == null) {
      this.level = 1;
    } else {
      this.level = parent.level + 1;
    }
    navigationKeyPaths = Util.buildKeyPath(jpaNavigationTargetType);
    this.navigationPath = navigationPath;
    this.jpaNavigationTargetType = jpaNavigationTargetType;
    if (navigationPath != null && navigationPath.getLeaf().getStructuredType() != jpaNavigationTargetType) {
      throw new IllegalStateException("navigation target must be the same as given target type");
    }
    if (navigationPath != null) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("#");
      boolean first = true;
      for (final JPAAttribute<?> pathElement : navigationPath.getPathElements()) {
        if (!first) {
          buffer.append(JPASelector.PATH_SEPERATOR);
        }
        buffer.append(pathElement.getExternalName());
        first = false;
      }
      buffer.append("->");
      buffer.append(jpaNavigationTargetType.getExternalName());
      this.targetLabel = buffer.toString();
    } else {
      this.targetLabel = jpaNavigationTargetType.getExternalName();
    }
  }

  public NavigationKeyBuilder buildChildNavigation(final JPANavigationPath navigationPath,
      final JPAStructuredType jpaNavigationResultType) throws ODataJPAModelException {
    return new NavigationKeyBuilder(this, navigationPath, jpaNavigationResultType);
  }

  public final JPAStructuredType getNavigationTargetType() {
    return jpaNavigationTargetType;
  }

  /**
   *
   * @return The parent or <code>null</code> if no parent existing.
   */
  public NavigationKeyBuilder getParentNavigationKeyPath() {
    return parent;
  }

  public List<JPASelector> getNavigationKeyPaths() {
    return navigationKeyPaths;
  }

  public String getNavigationAliasPrefix() {
    if (navigationPath == null) {
      return "k".concat(Integer.toString(level));// use as default
    }
    return navigationPath.getAlias().concat(Integer.toString(level));
  }

  /**
   *
   * @return The navigation path or <code>null</code> if root entity without navigation.
   */
  public JPANavigationPath getNavigationPath() {
    return navigationPath;
  }

  /**
   * The key is build from parent selected JOIN's and the declared key attribute/columns of given row.
   *
   * @param row The owning entity/type row stored in a parent {@link AbstractEntityQueryResult}.
   */
  public String buildKeyForNavigationOwningRow(final Tuple row) {
    final StringBuilder buffer = new StringBuilder();
    if (parent != null) {
      // for the parent we are an 'target' and have to use the key selection join with alias
      buffer.append(parent.buildKeyForNavigationTargetRow(row));
      buffer.append(JPASelector.PATH_SEPERATOR);
    }
    boolean first = true;
    for (final JPASelector jpaPath : navigationKeyPaths) {
      if (!first) {
        buffer.append('|');
      }
      // no alias prefix here, because is the direct name from selection query of entity
      final String alias = jpaPath.getAlias();
      final Object keyValue = row.get(alias);
      buffer.append(keyValue);
      first = false;
    }
    return buffer.toString();
  }

  /**
   * The key is build from selected JOIN based on {@link #getNavigationAliasPrefix()}.
   *
   * @param row The Child row stored as target (result) of an navigation.
   */
  public String buildKeyForNavigationTargetRow(final Tuple row) {
    final String aliasPrefix = (getNavigationAliasPrefix() != null) ? getNavigationAliasPrefix() : "";
    final StringBuilder buffer = new StringBuilder();
    if (parent != null) {
      buffer.append(parent.buildKeyForNavigationTargetRow(row));
      buffer.append(JPASelector.PATH_SEPERATOR);
    }
    boolean first = true;
    for (final JPASelector jpaPath : navigationKeyPaths) {
      if (!first) {
        buffer.append('|');
      }
      final String alias = aliasPrefix.concat(jpaPath.getAlias());
      final Object keyValue = row.get(alias);
      buffer.append(keyValue);
      first = false;
    }
    return buffer.toString();
  }

  /**
   *
   * @return A human readable name/title/label for the navigation represented by this key builder.
   * @see #toString()
   */
  public String getNavigationLabel() {
    final StringBuilder buffer = new StringBuilder();
    if (parent != null) {
      buffer.append(parent.getNavigationLabel());
    }
    buffer.append(targetLabel);
    return buffer.toString();
  }
}
