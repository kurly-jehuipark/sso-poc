package com.kurly.poc.sso.gluu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@RestController
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class GluuPocApplication {

  private static final Logger log = LoggerFactory.getLogger(GluuPocApplication.class);


  private ObjectMapper objectMapper;

  public static void main(String[] args) {
    SpringApplication.run(GluuPocApplication.class, args);
  }

  @RequestMapping("/authorize/callback")
  public Object test(@RequestParam Map<?, ?> param) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

    log.info("authorize - callback");

    if (param.isEmpty()) {
      return Map.of("authorize_response", "empty");
    }

    RestTemplate restTemplate = sslIgnoreRestTemplate();

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("scope", param.get("scope"));
    body.add("code", param.get("code"));
    body.add("grant_type", "authorization_code");
    body.add("redirect_uri", "http://localhost:8080/oauth/test");

    Map<String, Object> out = new HashMap<>();
    out.put("authorize_response", param);

    try {
      @SuppressWarnings("Convert2Diamond")
      ResponseEntity<HashMap<?, ?>> response = restTemplate.exchange(
          RequestEntity
              .post(URI.create("https://sso1.infra.kurlycorp.kr/oxauth/restv1/token"))
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                  "51d2eade-bb8e-405b-b5e9-93dc11710d43:UxxmtOfXJixevkLhEn38Hpte".getBytes()))
              .body(body),
          new ParameterizedTypeReference<HashMap<?, ?>>() {
          });
      out.put("token_reponse", response.getBody());

      @SuppressWarnings("ConstantConditions")
      final String accessToken = (String) response.getBody().get("access_token");
      final String payload =
          new String(Base64.getDecoder().decode(accessToken.split("\\.")[1]));

      out.put("access_token_decoding", objectMapper.readValue(payload, HashMap.class));

    } catch (HttpClientErrorException e) {
      out.put("token_response",
          objectMapper.readValue(
              e.getResponseBodyAsString(), new TypeReference<HashMap<String, Object>>() {
              }));
    } catch (RestClientException e) {
      log.error("error", e);
    }

    return out;
  }

  @RequestMapping("/oauth/test")
  public Object test2(@RequestParam Map<?, ?> param) {
    log.info("test = {}", param);
    return param;
  }

  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private RestTemplate sslIgnoreRestTemplate()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

    SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
        .loadTrustMaterial(null, acceptingTrustStrategy)
        .build();

    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

    CloseableHttpClient httpClient = HttpClients.custom()
        .setSSLSocketFactory(csf)
        .build();

    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();

    requestFactory.setHttpClient(httpClient);
    return new RestTemplate(requestFactory);
  }
}
