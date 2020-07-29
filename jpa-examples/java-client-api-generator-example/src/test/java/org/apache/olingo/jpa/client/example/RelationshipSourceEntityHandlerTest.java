package org.apache.olingo.jpa.client.example;

import org.apache.olingo.jpa.client.example.util.HandlerTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityAbstractHandler;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityDto;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityMeta;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityURIBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RelationshipSourceEntityHandlerTest extends HandlerTestBase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testLoadManyRelationship() throws Exception {
    final RelationshipSourceEntityAbstractHandler handler = createLocalEntityAccess(
        RelationshipSourceEntityAbstractHandler.class);
    final RelationshipSourceEntityURIBuilder uriBuilder = handler.defineEndpoint().appendKeySegment(Integer.valueOf(1))
        .expand(RelationshipSourceEntityMeta.LEFTM2NS_NAME, RelationshipSourceEntityMeta.UNIDIRECTIONALTARGETS_NAME,
            RelationshipSourceEntityMeta.TARGETS_NAME);

    final RelationshipSourceEntityDto dto =handler.retrieve(uriBuilder);
    Assert.assertNotNull(dto);
    Assert.assertNull(dto.getSecondLeftM2Ns());

    Assert.assertNotNull(dto.getLeftM2Ns());
    Assert.assertEquals(1, dto.getLeftM2Ns().size());
    Assert.assertEquals(Integer.valueOf(5), dto.getLeftM2Ns().iterator().next().getID());

    Assert.assertNotNull(dto.getUnidirectionalTargets());
    Assert.assertEquals(2, dto.getUnidirectionalTargets().size());
    Assert.assertEquals(Integer.valueOf(2), dto.getUnidirectionalTargets().iterator().next().getID());

    Assert.assertNotNull(dto.getTargets());
    Assert.assertEquals(dto.getTargets().size(), dto.getUnidirectionalTargets().size());
    Assert.assertEquals(dto.getTargets().iterator().next().getID(), dto.getUnidirectionalTargets().iterator().next()
        .getID());
  }


}
