package com.koch.ambeth.merge.util.setup;

/*-
 * #%L
 * jambeth-merge
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

import com.koch.ambeth.ioc.IInitializingModule;
import com.koch.ambeth.ioc.annotation.FrameworkModule;
import com.koch.ambeth.ioc.config.IBeanConfiguration;
import com.koch.ambeth.ioc.factory.IBeanContextFactory;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;

@FrameworkModule
public class SetupModule implements IInitializingModule {
	@LogInstance
	private ILogger log;

	@Override
	public void afterPropertiesSet(IBeanContextFactory beanContextFactory) throws Throwable {
		beanContextFactory.registerBean(DataSetup.class).autowireable(IDataSetup.class,
				IDatasetBuilderExtendable.class);
	}

	public static void registerDataSetBuilder(IBeanContextFactory beanContextFactory,
			Class<? extends IDatasetBuilder> type) {
		IBeanConfiguration builder = beanContextFactory.registerBean(type).autowireable(type);
		beanContextFactory.link(builder).to(IDatasetBuilderExtendable.class);
	}
}
