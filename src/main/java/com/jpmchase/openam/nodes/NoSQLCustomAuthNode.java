/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */

package com.jpmchase.openam.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.core.realms.Realm;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.jpmchase.openam.utils.RestClient;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * A node that checks to see if zero-page login headers have specified username
 * and whether that username is in a group permitted to use zero-page login
 * headers.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = NoSQLCustomAuthNode.Config.class)
public class NoSQLCustomAuthNode extends AbstractDecisionNode {

	private final Pattern DN_PATTERN = Pattern.compile("^[a-zA-Z0-9]=([^,]+),");
	private final Logger logger = LoggerFactory.getLogger(NoSQLCustomAuthNode.class);
	private final Config config;
	private final Realm realm;
	private Map<String, String> userMap = null;

	/**
	 * Configuration for the node.
	 */
	public interface Config {
		/**
		 * The header name for zero-page login that will contain the identity's
		 * username.
		 */
		@Attribute(order = 100)
		default String usernameHeader() {
			return "X-OpenAM-Username";
		}

		/**
		 * The header name for zero-page login that will contain the identity's
		 * password.
		 */
		@Attribute(order = 200)
		default String passwordHeader() {
			return "X-OpenAM-Password";
		}

		/*
		 * /** The group name (or fully-qualified unique identifier) for the group that
		 * the identity must be in.
		 * 
		 * @Attribute(order = 300) default String groupName() { return
		 * "zero-page-login"; }
		 */

		@Attribute(order = 300)
		String restEndURL();

	}

	/**
	 * Create the node using Guice injection. Just-in-time bindings can be used to
	 * obtain instances of other classes from the plugin.
	 *
	 * @param config The service config.
	 * @param realm  The realm the node is in.
	 * @throws NodeProcessException If the configuration was not valid.
	 */
	@Inject
	public NoSQLCustomAuthNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
		this.config = config;
		this.realm = realm;
	}

	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.debug("NoSQLCustomAuthNode processing started...");
		logger.debug(" The Treecontext node=====  " + context.toString());

		String userName = context.sharedState.get(USERNAME).asString();
		String password = getPassword(context);

		System.out.println("#process--> The username and password: " + userName + " " + password);

		// Logic to authenticate to Cassandra DB/ Here as of now Mock REST API

		try {

			Map<String, String> responseMap = authenticateUserInternalAPI(config.restEndURL(), userName, password);
			//Map<String, String> responseMap= authenticateUserExternalAPI(config.restEndURL(), userName, password);
			logger.debug("Message --------------->" + responseMap.get("message"));
			logger.debug("Code --------------->" + responseMap.get("code"));
			if ((responseMap.get("code").equals("0") || responseMap.get("code").contains("200 OK"))) {

				logger.debug("Successfully authenticated user !!!");
				System.out.println("#process--> Successfully authenticated user !!!");
				 /*return goTo(true).replaceSharedState(context.sharedState.copy().put(USERNAME,
				 userName)).replaceTransientState(context.transientState.copy().put(PASSWORD,
				 password)).build(); */
				return goTo(true).build();

			} else {
				logger.info("Authentication Failed !!!");
				System.out.println("Authentication Failed !!!");
				return goTo(false).build();
			}

		}

		/*
		 * catch (AuthenticationException e) {
		 * logger.error("Error authenticating user '{}' ", userName, e); }
		 */

		catch (Exception e) {
			logger.error("Error authenticating user '{}' ", userName, e);
		}
		/*
		 * catch (HttpClientErrorException e) {
		 * logger.error("Error authenticating user '{}'" + e.getMessage()); }
		 */

		logger.debug(" Authentication Failed !!!");
		return goTo(false).build();
	}

	private String getPassword(TreeContext context) throws NodeProcessException {
		String password = context.transientState.get(PASSWORD).asString();
		if (password == null) {
			logger.error("Password is null.");
			throw new NodeProcessException("Unable to authenticate");
		}
		return password;
	}

	private Map<String, String> authenticateUserInternalAPI(String restURL, String userName, String password)
			throws JSONException {
		logger.trace("In #authenticateUserInternalAPI method");

		// External API
		userMap = new HashMap<>();
		userMap = RestClient.authenticateUserAgainstInternalAPI(restURL, userName, password, userMap);
		return userMap;

	}

	private Map<String, String> authenticateUserExternalAPI(String restURL, String userName, String password)
			throws JSONException {
		logger.trace("In #authenticateUserExternalAPI method");

		// External API
		userMap = new HashMap<>();
		userMap.put("email", userName);
		userMap.put("password", password);
		userMap = RestClient.authenticateUserAgainstExternalAPI(restURL, userMap);
		return userMap;

	}

	private boolean isMemberOfGroup(AMIdentity userIdentity, String groupName) {
		try {
			Set<String> userGroups = userIdentity.getMemberships(IdType.GROUP);
			for (String group : userGroups) {
				if (groupName.equals(group)) {
					return true;
				}
				Matcher dnMatcher = DN_PATTERN.matcher(group);
				if (dnMatcher.find() && dnMatcher.group(1).equals(groupName)) {
					return true;
				}
			}
		} catch (IdRepoException | SSOException e) {
			logger.warn("Could not load groups for user {}", userIdentity);
		}
		return false;
	}
}
