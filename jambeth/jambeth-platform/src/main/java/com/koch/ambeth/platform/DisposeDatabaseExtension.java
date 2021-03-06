package com.koch.ambeth.platform;

/*-
 * #%L
 * jambeth-platform
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

import com.koch.ambeth.ioc.IServiceContext;
import com.koch.ambeth.persistence.api.IDatabase;
import com.koch.ambeth.persistence.api.database.IDatabaseProvider;

public class DisposeDatabaseExtension {
	public void disposeDatabase(IServiceContext beanContext) {
		IDatabaseProvider databaseProvider = beanContext.getService(
				IDatabaseProvider.DEFAULT_DATABASE_PROVIDER_NAME, IDatabaseProvider.class, false);
		IDatabase database = databaseProvider != null ? databaseProvider.tryGetInstance() : null;
		if (database != null) {
			database.dispose();
		}
	}
}
