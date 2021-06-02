package org.apache.olingo.jpa.servlet.springboot.bean.example;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SimpleIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testRunningServlet() throws Exception {
    final ResponseEntity<String> response = restTemplate.getForEntity("/odata/$metadata", String.class);
    Assert.assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCodeValue());
  }
}
