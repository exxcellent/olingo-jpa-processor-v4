package org.apache.olingo.jpa.client.example;

import org.apache.olingo.jpa.client.example.util.HandlerTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityAbstractHandler;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityDto;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityMeta;
import org.apache.olingo.jpa.processor.core.testmodel.RelationshipSourceEntityURIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RelationshipSourceEntityHandlerTest extends HandlerTestBase {

  @Test
  public void testLoadManyRelationship() throws Exception {
    final RelationshipSourceEntityAbstractHandler handler = createLocalEntityAccess(
        RelationshipSourceEntityAbstractHandler.class);
    final RelationshipSourceEntityURIBuilder uriBuilder = handler.defineEndpoint().appendKeySegment(Integer.valueOf(1))
        .expand(RelationshipSourceEntityMeta.LEFTM2NS_NAME, RelationshipSourceEntityMeta.UNIDIRECTIONALTARGETS_NAME,
            RelationshipSourceEntityMeta.TARGETS_NAME);

    final RelationshipSourceEntityDto dto =handler.retrieve(uriBuilder);
    Assertions.assertNotNull(dto);
    Assertions.assertNull(dto.getSecondLeftM2Ns());

    Assertions.assertNotNull(dto.getLeftM2Ns());
    Assertions.assertEquals(1, dto.getLeftM2Ns().size());
    Assertions.assertEquals(Integer.valueOf(5), dto.getLeftM2Ns().iterator().next().getID());

    Assertions.assertNotNull(dto.getUnidirectionalTargets());
    Assertions.assertEquals(2, dto.getUnidirectionalTargets().size());
    Assertions.assertEquals(Integer.valueOf(2), dto.getUnidirectionalTargets().iterator().next().getID());

    Assertions.assertNotNull(dto.getTargets());
    Assertions.assertEquals(dto.getTargets().size(), dto.getUnidirectionalTargets().size());
    Assertions.assertEquals(dto.getTargets().iterator().next().getID(), dto.getUnidirectionalTargets().iterator().next()
        .getID());
  }


}
