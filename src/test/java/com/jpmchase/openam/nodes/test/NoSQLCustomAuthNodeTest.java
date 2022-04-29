package com.jpmchase.openam.nodes.test;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.core.realms.Realm;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.jpmchase.openam.nodes.NoSQLCustomAuthNode;
import com.jpmchase.openam.nodes.NoSQLCustomAuthNode.Config;

public class NoSQLCustomAuthNodeTest {
	
	private final Logger logger = LoggerFactory.getLogger(NoSQLCustomAuthNodeTest.class);
	Config config;

	Realm realm;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Inject
	NoSQLCustomAuthNode node;

	/*@Test
	public void testProcessPassesUsernameAndPasswordToExternalRestAPI() throws Exception {
		// Given
		JsonValue sharedState = json(object(field(USERNAME, "Developer5@gmail.com")));
		JsonValue transientState = json(object(field(PASSWORD, "123456")));

		config = whenNodeConfigHasAttributes("http://restapi.adequateshop.com/api/authaccount/login");
		realm = whenNodeRealmHasAttributes();
		node = new NoSQLCustomAuthNode(config, realm);

		// When
		Action result = node.process(getContext(sharedState, transientState));
		logger.info("Node Outcome : " + result.outcome);
		logger.info("Complete Node Result: " + result);
		logger.info(" Callbacks: "+ result.callbacks);
		logger.info(" Callbacks: "+ result.sharedState.get("username"));

		// Then
		assertEquals("true", result.outcome);
		//assertNull("", result.callbacks);
		//assertThat(result.callbacks).isEmpty();
		//assertSame(result.sharedState, sharedState);
		//assertSame(result.transientState, transientState);
		//assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
		//assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "tom"), entry(REALM, "/realm"));
		//assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "123"));

	} */
	
	@Test
	public void testProcessPassesUsernameAndPasswordToInternalRestAPI() throws Exception {
		// Given
		JsonValue sharedState = json(object(field(USERNAME, "to")));
		JsonValue transientState = json(object(field(PASSWORD, "123")));

		config = whenNodeConfigHasAttributes("http://sso-am.ey.net:8080/v2/users");
		realm = whenNodeRealmHasAttributes();
		node = new NoSQLCustomAuthNode(config, realm);

		// When
		Action result = node.process(getContext(sharedState, transientState));
		logger.info("Node Outcome : " + result.outcome);
		logger.info("Complete Node Result: " + result);

		// Then
		assertEquals("true", result.outcome);
		//assertNull("", result.callbacks);
		//assertThat(result.callbacks).isEmpty();
		assertSame(result.sharedState, sharedState);
		assertSame(result.transientState, transientState);
		//assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
		//assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "tom"), entry(REALM, "/realm"));
		//assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "123"));

	} 
	
	
	private Config whenNodeConfigHasAttributes(String restURL) {
		config = mock(Config.class);
		given(config.restEndURL()).willReturn(restURL);
		return config;
	}

	private Realm whenNodeRealmHasAttributes() {
		realm = mock(Realm.class);
		given(realm.asPath()).willReturn("/realmA/realmB");
		return realm;
	}

	private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
		return new TreeContext(sharedState, transientState, new Builder().build(), emptyList(), Optional.empty());
	}
}
