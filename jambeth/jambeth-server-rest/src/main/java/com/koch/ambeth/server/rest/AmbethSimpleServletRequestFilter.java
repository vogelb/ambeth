package com.koch.ambeth.server.rest;

/*-
 * #%L
 * jambeth-server-rest
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.ext.Provider;

import com.koch.ambeth.ioc.IServiceContext;
import com.koch.ambeth.ioc.threadlocal.IThreadLocalCleanupController;
import com.koch.ambeth.util.IConversionHelper;
import com.koch.ambeth.util.config.IProperties;
import com.koch.ambeth.util.state.IStateRollback;

/**
 * A simple servlet request filter. Just ensures thread local variables are cleared after requests
 * are handled.
 */
@Provider
public class AmbethSimpleServletRequestFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// intended blank
	}

	@Override
	public void destroy() {
		// intended blank
	}

	/**
	 * This method always clear thread local variables after a hhtp request was handled. Override
	 * {@link #doFilterIntern(ServletRequest, ServletResponse, FilterChain)} when deriving this class.
	 *
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
	 *      javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		IThreadLocalCleanupController threadLocalCleanupController =
				getService(request.getServletContext(), IThreadLocalCleanupController.class);
		IStateRollback rollback =
				threadLocalCleanupController.pushThreadLocalState(IStateRollback.EMPTY_ROLLBACKS);
		try {
			doFilterIntern(request, response, chain);
		}
		finally {
			rollback.rollback();
		}
	}

	/**
	 * Override this method when deriving class {@link AmbethSimpleServletRequestFilter}. This method
	 * is called by {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} which always clear
	 * thread locals.
	 */
	protected void doFilterIntern(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		chain.doFilter(request, response);
	}

	protected <T> T getProperty(ServletContext servletContext, Class<T> propertyType,
			String propertyName) {
		Object value = getService(servletContext, IProperties.class).get(propertyName);
		return getService(servletContext, IConversionHelper.class).convertValueToType(propertyType,
				value);
	}

	protected <T> T getService(ServletContext servletContext, Class<T> serviceType) {
		return getServiceContext(servletContext).getService(serviceType);
	}

	protected <T> T getService(ServletContext servletContext, String beanName, Class<T> serviceType) {
		return getServiceContext(servletContext).getService(beanName, serviceType);
	}

	/**
	 *
	 * @return The singleton IServiceContext which is stored in the context of the servlet
	 */
	protected IServiceContext getServiceContext(ServletContext servletContext) {
		return (IServiceContext) servletContext
				.getAttribute(AmbethServletListener.ATTRIBUTE_I_SERVICE_CONTEXT);
	}
}
