package org.apache.olingo.jpa.servlet.springboot.bean.example;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SimpleIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testRunningServlet() throws Exception {
    final ResponseEntity<String> response = restTemplate.getForEntity("/odata/$metadata", String.class);
    Assertions.assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode().value());
  }
}
