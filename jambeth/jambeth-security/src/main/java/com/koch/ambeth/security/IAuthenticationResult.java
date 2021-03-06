package com.koch.ambeth.security;

import com.koch.ambeth.util.IImmutableType;

/*-
 * #%L
 * jambeth-security
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

/**
 * Encapsulates the result of an authentication step executed by a {@link IAuthenticationManager}.
 * This result is used as a further information to execute the authorization step with the
 * {@link IAuthorizationManager}.
 */
public interface IAuthenticationResult extends IImmutableType {
	String getSID();

	boolean isChangePasswordRecommended();

	boolean isChangePasswordRequired();

	boolean isRehashPasswordRecommended();

	boolean isAccountingActive();
}
