package com.koch.ambeth.security.server.proxy;

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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import com.koch.ambeth.ioc.IBeanRuntime;
import com.koch.ambeth.ioc.IOrderedBeanProcessor;
import com.koch.ambeth.ioc.IServiceContext;
import com.koch.ambeth.ioc.ProcessorOrder;
import com.koch.ambeth.ioc.config.IBeanConfiguration;
import com.koch.ambeth.ioc.factory.IBeanContextFactory;
import com.koch.ambeth.security.PasswordType;
import com.koch.ambeth.security.SecurityContext;
import com.koch.ambeth.security.SecurityContextPassword;
import com.koch.ambeth.security.SecurityContextScope;
import com.koch.ambeth.security.SecurityContextUserName;
import com.koch.ambeth.security.StringSecurityScope;
import com.koch.ambeth.security.server.SecurityFilterInterceptor;
import com.koch.ambeth.security.server.SecurityFilterInterceptor.SecurityMethodMode;
import com.koch.ambeth.service.model.ISecurityScope;
import com.koch.ambeth.service.proxy.AbstractCascadePostProcessor;
import com.koch.ambeth.service.proxy.IBehaviorTypeExtractor;
import com.koch.ambeth.service.proxy.IMethodLevelBehavior;
import com.koch.ambeth.service.proxy.MethodLevelBehavior;
import com.koch.ambeth.util.annotation.AnnotationCache;
import com.koch.ambeth.util.proxy.ICascadedInterceptor;

public class SecurityPostProcessor extends AbstractCascadePostProcessor
		implements IOrderedBeanProcessor {
	protected AnnotationCache<SecurityContext> annotationCache = new AnnotationCache<SecurityContext>(
			SecurityContext.class) {
		@Override
		protected boolean annotationEquals(SecurityContext left, SecurityContext right) {
			return Objects.equals(left.value(), right.value());
		}
	};

	protected final IBehaviorTypeExtractor<SecurityContext, SecurityMethodMode> btExtractor =
			new IBehaviorTypeExtractor<SecurityContext, SecurityMethodMode>() {
				@Override
				public SecurityMethodMode extractBehaviorType(SecurityContext annotation,
						AnnotatedElement annotatedElement) {
					if (!(annotatedElement instanceof Method)) {
						return new SecurityMethodMode(annotation.value());
					}
					Method method = (Method) annotatedElement;
					Annotation[][] parameterAnnotations = method.getParameterAnnotations();
					int userNameIndex = -1;
					int passwordIndex = -1;
					PasswordType passwordType = null;
					int securityScopeIndex = -1;
					for (int a = parameterAnnotations.length; a-- > 0;) {
						for (Annotation annotationOfParam : parameterAnnotations[a]) {
							if (annotationOfParam instanceof SecurityContextUserName) {
								if (userNameIndex != -1) {
									throw new IllegalStateException(
											"Annotation '" + SecurityContextUserName.class.getName()
													+ "' ambiguous on method signature '" + method.toGenericString() + "'");
								}
								userNameIndex = a;
							}
							else if (annotationOfParam instanceof SecurityContextPassword) {
								if (passwordIndex != -1) {
									throw new IllegalStateException(
											"Annotation '" + SecurityContextPassword.class.getName()
													+ "' ambiguous on method signature '" + method.toGenericString() + "'");
								}
								passwordIndex = a;
								passwordType = ((SecurityContextPassword) annotationOfParam).value();
							}
							else if (annotationOfParam instanceof SecurityContextScope) {
								if (securityScopeIndex != -1) {
									throw new IllegalStateException(
											"Annotation '" + SecurityContextScope.class.getName()
													+ "' ambiguous on method signature '" + method.toGenericString() + "'");
								}
								securityScopeIndex = a;
							}
						}
					}
					ISecurityScope securityScope =
							securityScopeIndex == -1 ? StringSecurityScope.DEFAULT_SCOPE
									: null;
					return new SecurityMethodMode(annotation.value(), userNameIndex, passwordIndex,
							passwordType,
							securityScopeIndex, securityScope);
				}
			};

	@Override
	protected ICascadedInterceptor handleServiceIntern(IBeanContextFactory beanContextFactory,
			IServiceContext beanContext, IBeanConfiguration beanConfiguration, Class<?> type,
			Set<Class<?>> requestedTypes) {
		IMethodLevelBehavior<SecurityMethodMode> behaviour = MethodLevelBehavior.create(type,
				annotationCache, SecurityMethodMode.class, btExtractor, beanContextFactory, beanContext);
		if (behaviour == null) {
			return null;
		}
		SecurityFilterInterceptor interceptor = new SecurityFilterInterceptor();
		if (beanContext.isRunning()) {
			IBeanRuntime<SecurityFilterInterceptor> interceptorBC = beanContext
					.registerWithLifecycle(interceptor);
			interceptorBC.propertyValue(SecurityFilterInterceptor.P_METHOD_LEVEL_BEHAVIOUR, behaviour);
			return interceptorBC.finish();
		}
		IBeanConfiguration interceptorBC = beanContextFactory.registerWithLifecycle(interceptor);
		interceptorBC.propertyValue(SecurityFilterInterceptor.P_METHOD_LEVEL_BEHAVIOUR, behaviour);
		return interceptor;

	}

	@Override
	public ProcessorOrder getOrder() {
		return ProcessorOrder.HIGHER;
	}
}
