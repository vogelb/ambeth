package com.koch.ambeth.persistence;

/*-
 * #%L
 * jambeth-persistence
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

import java.util.Collection;
import java.util.List;

import com.koch.ambeth.util.appendable.IAppendable;
import com.koch.ambeth.util.collections.IList;

public interface IPersistenceHelper {
	IList<String> buildStringListOfValues(List<?> ids);

	String buildStringOfValues(List<?> ids);

	IAppendable appendStringOfValues(List<?> ids, IAppendable sb);

	IList<IList<Object>> splitValues(List<?> ids);

	IList<IList<Object>> splitValues(List<?> values, int batchSize);

	IList<IList<Object>> splitValues(Collection<?> ids);

	IAppendable appendSplittedValues(String idColumnName, Class<?> fieldType, List<?> ids,
			List<Object> parameters, IAppendable sb);
}
