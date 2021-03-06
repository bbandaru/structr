/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;


/**
 * Utility class for authentication
 *
 *
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";

	private static final Logger logger            = LoggerFactory.getLogger(AuthHelper.class.getName());

	//~--- get methods ----------------------------------------------------

	/**
	 * Find a {@link Principal} for the given credential
	 *
	 * @param key
	 * @param value
	 * @return principal
	 */
	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value) {

		if (value != null) {

			try {

				return StructrApp.getInstance().nodeQuery(Principal.class).and(key, value).getFirst();

			} catch (FrameworkException fex) {

				logger.warn("Error while searching for principal", fex);
			}

		}

		return null;
	}

	/**
	 * Find a {@link Principal} with matching password and given key or name
	 *
	 * @param key
	 * @param value
	 * @param password
	 * @return principal
	 * @throws AuthenticationException
	 */
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal principal  = null;

		// FIXME: this might be slow, because the the property file needs to be read each time
		final String superuserName = StructrApp.getConfigurationValue(Services.SUPERUSER_USERNAME);
		final String superUserPwd  = StructrApp.getConfigurationValue(Services.SUPERUSER_PASSWORD);

		if (superuserName.equals(value) && superUserPwd.equals(password)) {

			// logger.info("############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else {

			try {

				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractUser.name, value).getFirst();

				if (principal == null) {

					logger.info("No principal found for {} {}", new Object[]{ key.dbName(), value });

					errorMsg = STANDARD_ERROR_MSG;

				} else {

					if (principal.getProperty(Principal.blocked)) {

						logger.info("Principal {} is blocked", principal);

						errorMsg = STANDARD_ERROR_MSG;

					}

					if (StringUtils.isEmpty(password)) {

						logger.info("Empty password for principal {}", principal);

						errorMsg = "Empty password, should never happen here!";

					} else {

						// let Principal decide how to check password
						if (!principal.isValidPassword(password)) {

							errorMsg = STANDARD_ERROR_MSG;
						}

					}

				}

			} catch (FrameworkException fex) {

				logger.warn("", fex);

			}

		}

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);
		}

		return principal;

	}

	/**
	 * Find a {@link Principal} for the given session id
	 *
	 * @param sessionId
	 * @return principal
	 */
	public static Principal getPrincipalForSessionId(final String sessionId) {

		return getPrincipalForCredential(Principal.sessionIds, new String[]{ sessionId });

	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		HttpSession session = request.getSession(false);

		if (session == null) {
			session = SessionHelper.newSession(request);
		}

		SessionHelper.clearInvalidSessions(user);

		// We need a session to login a user
		if (session != null) {

			SessionHelper.clearSession(session.getId());
			user.addSessionId(session.getId());

			Actions.call(Actions.NOTIFICATION_LOGIN, user);
		}
	}

	public static void doLogout(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final HttpSession session = request.getSession(false);

		SessionHelper.clearSession(session.getId());

		SessionHelper.invalidateSession(session);

		AuthHelper.sendLogoutNotification(user);

	}

	public static void sendLogoutNotification (final Principal user) throws FrameworkException {

		Actions.call(Actions.NOTIFICATION_LOGOUT, user);

	}

}
