/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;

public class AuthRealm extends AuthorizingRealm {

	private static Logger log = LoggerFactory.getLogger(AuthRealm.class);

	public static class AuthMatcher extends PasswordMatcher {

		@Override
		public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

			//TODO: remove plain text match in final version
			Object plain = getSubmittedPassword(token);
			Object saved = getStoredPassword(info);
			AuthService service = AuthService.getInstance();

			if (plain instanceof char[]) {
				plain = new String((char[]) plain);
			}

			if (service.match((String) plain, (String) saved) || super.doCredentialsMatch(token, info)) {
				return true;
			}
			
			log.error("Password authentication failed for user: {}", token.getPrincipal());
			return false;
		}
	}

	private CredentialsMatcher credentialsMatcher = new AuthMatcher();

	@Override
	public CredentialsMatcher getCredentialsMatcher() {
		return credentialsMatcher;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

		final String code = ((UsernamePasswordToken) token).getUsername();
		final User user = AuthUtils.getUser(code);
		if (user == null || !AuthUtils.isActive(user)) {
			return null;
		}

		return new SimpleAuthenticationInfo(code, user.getPassword(), getName());
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

		final String code = (String) principals.fromRealm(getName()).iterator().next();
		final User user = AuthUtils.getUser(code);

		if (user == null) {
			return null;
		}

		final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		final Group group = user.getGroup();
		if (group != null) {
			info.addRole(group.getCode());
		}

		return info;
	}
}
