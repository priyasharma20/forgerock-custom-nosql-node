package com.jpmchase.openam.utils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


public final class RestClient {
	private final static Logger logger = LoggerFactory.getLogger(RestClient.class);
	public static final String USER_NAME = "tom";
	public static final String PASSWORD = "123";

	static final String URL_EMPLOYEES = "http://localhost:8080/v2/users";

	public static void main(String[] args) {
		String url = "";
		// request body parameters
		try {
		Map<String, String> map = new HashMap<>();
		map=authenticateUserAgainstInternalAPI("http://localhost:8080/v2/users", "tom", "123", map);
		
		
		//authenticateUsingPost(map);
		}
		
		catch(HttpClientErrorException e) {
			logger.error("Authentication Failed "+ e.getMessage());
		}
		
	}

	private static void authenticateUsingPost(Map<String, String> map) {
		String url;
		try {
			
			map.put("email", "Developer5@gmail.com");
			map.put("password", "12356");
			url = "http://restapi.adequateshop.com/api/authaccount/login";
			map = authenticateUserAgainstExternalAPI(url, map);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Map<String, String> authenticateUserAgainstInternalAPI(String restURL, String userName, String password, Map<String, String> userMap) {
		// HttpHeaders
		HttpHeaders headers = new HttpHeaders();

		//
		// Authentication
		//
		String auth = userName + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
		String authHeader = "Basic " + new String(encodedAuth);
		headers.set("Authorization", authHeader);
		//
		headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		// Request to return JSON format
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("my_other_key", "my_other_value");

		// HttpEntity<String>: To get result as String.
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		// RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		// Send request with GET method, and Headers.
		ResponseEntity<String> response = restTemplate.exchange(restURL, HttpMethod.GET, entity, String.class);

		String result = response.getBody();

		//logger.info("The Response body: "+ result);
		System.out.println("Status Code:" +response.getStatusCode());
		//logger.info("Status Code:" +response.getStatusCode());
		//logger.info("Headers:" +response.getHeaders());
		
		userMap.put("code", response.getStatusCode().toString());
		userMap.put("message", result);
		return userMap;

	}

	public static Map<String, String> authenticateUserAgainstExternalAPI(String restURL, Map<String, String> map) throws JSONException {

		// create an instance of RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		// create headers
		HttpHeaders headers = new HttpHeaders();
		// set `content-type` header
		headers.setContentType(MediaType.APPLICATION_JSON);
		// set `accept` header
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		// build the request
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);

		// send POST request
		ResponseEntity<String> response = restTemplate.postForEntity(restURL, entity, String.class);

		logger.info("HTTP-Post response status code =" + response.getStatusCodeValue()
				+ response.getStatusCode());
		String responseBody = response.getBody();

		logger.info("Post response Body: " + responseBody);

		JSONObject jsonObject = new JSONObject(response.getBody());

		logger.info("Attribute Message Value = " + jsonObject.get("message"));

		map.put("code", jsonObject.getString("code"));
		map.put("message", jsonObject.get("message").toString());

		return map;
	}

}
