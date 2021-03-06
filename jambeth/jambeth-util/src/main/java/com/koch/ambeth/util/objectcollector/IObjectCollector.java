package com.koch.ambeth.util.objectcollector;

/*-
 * #%L
 * jambeth-util
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

public interface IObjectCollector {
	<T> T create(Class<T> type);

	void dispose(Object object);

	<T> void dispose(Class<T> type, T object);

	void cleanUp();

	/**
	 * Only use the result of this method within a controlled code block only.
	 *
	 * - Do not give it as an argument to other methods - Do never save the result on a static or
	 * object member - In other words: Use it on the stack and never on the heap
	 *
	 * @return
	 */
	IThreadLocalObjectCollector getCurrent();
}
