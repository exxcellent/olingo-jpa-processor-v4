package org.apache.olingo.jpa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.junit.Test;

public class TestPair extends TestMappingRoot {

  @Test
  public void testEquals() {

    final Pair<String, String> pair1 = new Pair<String, String>("left", "right");
    final Pair<String, String> pair2 = new Pair<String, String>("left", "right");
    final Pair<String, String> pair3 = new Pair<String, String>("right", "left");

    assertTrue(pair1.equals(pair1));
    assertTrue(pair1.equals(pair2));
    assertFalse(pair1.equals(pair3));
    assertFalse(pair1.equals(new Object()));
  }

}
