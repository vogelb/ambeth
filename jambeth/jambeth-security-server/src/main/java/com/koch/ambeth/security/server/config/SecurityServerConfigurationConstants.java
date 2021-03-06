package com.koch.ambeth.security.server.config;

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

import com.koch.ambeth.util.annotation.ConfigurationConstantDescription;
import com.koch.ambeth.util.annotation.ConfigurationConstants;

@ConfigurationConstants
public final class SecurityServerConfigurationConstants {
	/**
	 * Optional parameter.<br>
	 * Default = PBKDF2WithHmacSHA1
	 */
	@ConfigurationConstantDescription("Optional parameter. Default = PBKDF2WithHmacSHA1")
	public static final String LoginPasswordAlgorithmName = "security.login.password.algorithm.name";

	/**
	 * Optional parameter. Must be >0.<br>
	 * Default = 8192
	 */
	@ConfigurationConstantDescription("Optional parameter. Must be >0. Default = 8192")
	public static final String LoginPasswordAlgorithmIterationCount =
			"security.login.password.algorithm.iterationcount";

	/**
	 * Optional parameter. Must be >0.<br>
	 * Default = 160
	 */
	@ConfigurationConstantDescription("Optional parameter. Must be >0. Default = 160")
	public static final String LoginPasswordAlgorithmKeySize =
			"security.login.password.algorithm.keysize";

	/**
	 * Configures the feature to auto-rehash passwords on-the-fly to ensure long-term security of
	 * stored passwords.<br>
	 * Optional parameter.<br>
	 * Default = false
	 */
	@ConfigurationConstantDescription("Configures the feature to auto-rehash passwords on-the-fly to ensure long-term security of stored passwords. Optional parameter. Default = false")
	public static final String LoginPasswordAutoRehashActive =
			"security.login.password.autorehash.active";

	/**
	 * Configures the byte-length of generated passwords (e.g. for new user accounts).<br>
	 * Optional parameter. Must be >0.<br>
	 * Default = 16
	 */
	@ConfigurationConstantDescription("Configures the byte-length of generated passwords (e.g. for new user accounts). Optional parameter. Must be >0. Default = 16")
	public static final String LoginPasswordGeneratedLength =
			"security.login.password.generated.length";

	/**
	 * Configures the maximum lifetime (in days) after which a password should be changed.<br>
	 * Optional parameter. Must be >0.<br>
	 * Default = 30
	 */
	@ConfigurationConstantDescription("Configures the maximum lifetime (in days) after which a password should be changed. Optional parameter. Must be >0. Default = 30")
	public static final String LoginPasswordLifetime = "security.login.password.lifetime";

	@ConfigurationConstantDescription("Configures the password change recommendation (in days) before it is required to change. Default = 7")
	public static final String LoginPasswordChangeRecommendation =
			"security.login.password.changerecommendation";

	/**
	 * Configures the maximum password history which should be kept to 'help' a user to do a 'real'
	 * password change.<br>
	 * Optional parameter. Must be >=0.<br>
	 * Default = 10
	 */
	@ConfigurationConstantDescription("Configures the maximum password history which should be kept to 'help' a user to do a 'real' password change. Optional parameter. Must be >=0. Default = 10")
	public static final String LoginPasswordHistoryCount = "security.login.password.history.count";

	/**
	 * Configures the internal 'secret' which is used to decrypt encrypted salts. If you define the
	 * parameter ensure that it is NOT hard-coded in our source-code and NOT stored at the same place
	 * as the password-entity (e.g. do NOT store it in the database). The recommended place is an
	 * external property file in the file-system of the deployed application.<br>
	 * Optional parameter (as long as no encrypted salts exist).<br>
	 * Default = null
	 */
	@ConfigurationConstantDescription("Configures the internal 'secret' which is used to decrypt encrypted salts. If you define the parameter ensure that it is NOT hard-coded in our source-code and NOT stored at the same place as the password-entity (e.g. do NOT store it in the database). The recommended place is an external property file in the file-system of the deployed application. Optional parameter (as long as no encrypted salts exist). Default = null")
	public static final String LoginSaltPassword = "security.login.salt.password";

	/**
	 * Configures the algorithm which is used to encrypt salts. Value itself is not considered unless
	 * a specific salt password is defined.<br>
	 * Optional parameter.<br>
	 * Default = AES/CBC/PKCS5Padding
	 */
	@ConfigurationConstantDescription("Configures the algorithm which is used to encrypt salts. Value itself is not considered unless a specific salt password is defined. Optional parameter. Default = AES/CBC/PKCS5Padding")
	public static final String LoginSaltAlgorithmName = "security.login.salt.algorithm.name";

	/**
	 * Configures the keyspec name which is used to encrypt salts. Must be compatible to the
	 * configured 'security.login.salt.algorithm.name'. Value itself is not considered unless a
	 * specific salt password is defined.<br>
	 * Optional parameter.<br>
	 * Default = AES
	 */
	@ConfigurationConstantDescription("Configures the keyspec name which is used to encrypt salts. Must be compatible to the configured 'security.login.salt.algorithm.name'. Value itself is not considered unless a specific salt password is defined. Optional parameter. Default = AES")
	public static final String LoginSaltKeySpecName = "security.login.salt.keyspec.name";

	/**
	 * Configures the byte-length of the generated random salt each time a password gets created or
	 * rehashed.<br>
	 * Optional parameter. Must be >0.<br>
	 * Default = 16
	 */
	@ConfigurationConstantDescription("Configures the byte-length of the generated random salt each time a password gets created or rehashed. Optional parameter. Must be >0. Default = 16")
	public static final String LoginSaltLength = "security.login.salt.length";

	public static final String SignatureAlgorithmName = "security.signature.algorithm.name";

	public static final String SignatureKeyAlgorithmName = "security.signature.key.algorithm.name";

	/**
	 * Configures the bit-length of the generated public/private key pair. The security harness is
	 * related to the chosen SignatureAlgorithmName.<br>
	 * For example for RSA keys above 3072 bits are considered safe - with EC comparable safety is
	 * achieved with numbers around 512.<br>
	 * Optional parameter. Must be >=112.<br>
	 * Default = 512
	 */
	@ConfigurationConstantDescription("Configures the bit-length of the generated public/private key pair. The security harness is related to the chosen SignatureAlgorithmName. For example for RSA keys above 3072 bits are considered safe - with EC comparable safety is achieved with numbers around 512. Optional parameter. Must be >=112. Default = 512")
	public static final String SignatureKeySize = "security.signature.length";

	/**
	 * Configures the algorithm which is used to encrypt the private key of signatures.<br>
	 * The password of the encryption is the password of the user itself. So only the user can read
	 * its own private key at any point of time<br>
	 * Optional parameter.<br>
	 * Default = AES/CBC/PKCS5Padding
	 */
	@ConfigurationConstantDescription("Configures the algorithm which is used to encrypt salts. Value itself is not considered unless a specific salt password is defined. Optional parameter. Default = AES/CBC/PKCS5Padding")
	public static final String SignatureEncryptionAlgorithmName =
			"security.signature.encryption.algorithm.name";

	public static final String SignaturePaddedKeySize =
			"security.signature.encryption.keypadding.length";

	public static final String SignaturePaddedKeyAlgorithmName =
			"security.signature.padding.algorithm.name";

	/**
	 * Configures the keyspec name which is used to encrypt the private key of signatures. Must be
	 * compatible to the configured 'security.signature.encryption.algorithm.name'.<br>
	 * The password of the encryption is the password of the user itself. So only the user can read
	 * its own private key at any point of time<br>
	 * Optional parameter.<br>
	 * Default = AES
	 */
	@ConfigurationConstantDescription("Configures the keyspec name which is used to encrypt salts. Must be compatible to the configured 'security.login.salt.algorithm.name'. Value itself is not considered unless a specific salt password is defined. Optional parameter. Default = AES")
	public static final String SignatureEncryptionKeySpecName =
			"security.signature.encryption.keyspec.name";

	@ConfigurationConstantDescription("TODO")
	public static final String SignaturePaddedKeyIterationCount =
			"security.signature.encryption.algorithm.iterationcount";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionKeySpecName = "security.crypto.keyspec.name";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionAlgorithmName = "security.crypto.algorithm.name";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionPaddedKeyAlgorithmName =
			"security.crypto.paddedkey.algorithm.name";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionPaddedKeyIterationCount =
			"security.crypto.paddedkey.iterationcount";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionPaddedKeySize = "security.crypto.paddedkey.size";

	@ConfigurationConstantDescription("TODO")
	public static final String EncryptionPaddedKeySaltSize = "security.crypto.paddedkey.saltsize";

	@ConfigurationConstantDescription("TODO")
	public static final String SignatureActive = "security.signature.active";

	@ConfigurationConstantDescription("TODO")
	public static final String AuthenticationManagerType = "security.authmanager.type";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapDomain = "security.ldap.domain";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapHost = "security.ldap.host";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapBase = "security.ldap.base";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapUserAttribute = "security.ldap.userattribute";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapCtxFactory = "security.ldap.ctxfactory";

	@ConfigurationConstantDescription("TODO")
	public static final String LdapFilter = "security.ldap.filter";

	private SecurityServerConfigurationConstants() {
		// Intended blank
	}
}
