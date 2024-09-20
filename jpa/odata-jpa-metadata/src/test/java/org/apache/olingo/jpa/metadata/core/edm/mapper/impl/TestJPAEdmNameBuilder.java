package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestJPAEdmNameBuilder {
  private JPAEdmNameBuilder cut;

  @Test
  public void CheckBuildContainerNameSimple() {
    cut = new JPAEdmNameBuilder("cdw");
    assertEquals("CdwContainer", cut.buildContainerName());
  }

  @Test
  public void CheckBuildContainerNameComplex() {
    cut = new JPAEdmNameBuilder("org.apache.olingo.jpa");
    assertEquals("OrgApacheOlingoJpaContainer", cut.buildContainerName());
  }
}
