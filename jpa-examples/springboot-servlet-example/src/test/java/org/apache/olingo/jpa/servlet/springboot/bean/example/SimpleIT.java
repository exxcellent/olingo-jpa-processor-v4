package org.apache.olingo.jpa.servlet.springboot.bean.example;

import java.util.List;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.test.util.Constant;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@AutoConfigureRestTestClient
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimpleIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testRunningServlet() throws Exception {
    final ResponseEntity<String> response = restTemplate.getForEntity("/odata/$metadata", String.class);
    Assertions.assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode().value());
  }
  
  @Test
  public void testSpringBootParameterDeserializationInActionCall() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    String body = "{\"affectedPersons\": [], \"minAny\": 0, \"maxAny\": 3}";
    HttpEntity<String> request = new HttpEntity<>(body, headers);
    final ResponseEntity<String> response = restTemplate.exchange("/odata/Persons('99')/"+Constant.PUNIT_NAME +"."+"DoNothingAction1", HttpMethod.POST, request, String.class);
    Assertions.assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode().value());
    Assertions.assertNotNull(response.getBody());
    JSONObject jsonResponseBody = new JSONObject(response.getBody());
    Assertions.assertEquals(jsonResponseBody.get("ID"), "keyId...123");
  }
  
}
