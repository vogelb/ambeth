package com.koch.ambeth.security.server.auth;

/*-
 * #%L
 * jambeth-security-server
 * %%
 * Copyright (C) 2017 Koch Softwaredevelopment
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.merge.security.ISecurityActivation;
import com.koch.ambeth.security.AuthenticationException;
import com.koch.ambeth.security.AuthenticationResult;
import com.koch.ambeth.security.IAuthentication;
import com.koch.ambeth.security.IAuthenticationResult;
import com.koch.ambeth.security.model.IPassword;
import com.koch.ambeth.security.model.IUser;
import com.koch.ambeth.security.server.ICheckPasswordResult;
import com.koch.ambeth.security.server.IPasswordUtil;
import com.koch.ambeth.security.server.IUserIdentifierProvider;
import com.koch.ambeth.security.server.IUserResolver;
import com.koch.ambeth.security.server.config.SecurityServerConfigurationConstants;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.state.IStateRollback;

public class EmbeddedAuthenticationManager extends AbstractAuthenticationManager {
	@Autowired
	protected IUserIdentifierProvider userIdentifierProvider;

	@Autowired
	protected IUserResolver userResolver;

	@Autowired
	protected IPasswordUtil passwordUtil;

	@Autowired
	protected ISecurityActivation securityActivation;

	@Property(name = SecurityServerConfigurationConstants.LoginPasswordAutoRehashActive,
			defaultValue = "true")
	protected boolean autoRehashPasswords;

	@Override
	public IAuthenticationResult authenticate(final IAuthentication authentication)
			throws AuthenticationException {
		IUser user;
		try {
			IStateRollback rollback = securityActivation
					.pushWithoutSecurity(IStateRollback.EMPTY_ROLLBACKS);
			try {
				user = userResolver.resolveUserBySID(authentication.getUserName());
				if (user != null) {
					// enforce loading
					user.getPassword();
				}
			}
			finally {
				rollback.rollback();
			}
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		if (user == null || user.getPassword() == null) {
			throw createAuthenticationException(authentication);
		}
		try {
			final IPassword password = user.getPassword();
			final ICheckPasswordResult checkPasswordResult = passwordUtil
					.checkClearTextPassword(authentication.getPassword(), password);
			if (!checkPasswordResult.isPasswordCorrect()) {
				throw createAuthenticationException(authentication);
			}
			boolean rehashRecommended = checkPasswordResult.isRehashPasswordRecommended();
			if (rehashRecommended && autoRehashPasswords) {
				IStateRollback rollback = securityActivation
						.pushWithoutSecurity(IStateRollback.EMPTY_ROLLBACKS);
				try {
					passwordUtil.rehashPassword(authentication.getPassword(), password);
				}
				finally {
					rollback.rollback();
				}
				rehashRecommended = false;
			}
			String sid = userIdentifierProvider.getSID(user);
			return new AuthenticationResult(sid, checkPasswordResult.isChangePasswordRecommended(),
					checkPasswordResult.isChangePasswordRequired(), rehashRecommended,
					checkPasswordResult.isAccountingActive());
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}
}
