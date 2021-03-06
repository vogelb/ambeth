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

import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.koch.ambeth.util.collections.ILinkedMap;
import com.koch.ambeth.util.collections.LinkedHashMap;

public class ThreadLocalObjectCollector extends ThreadLocal<SingleThreadCollector>
		implements IObjectCollector, IThreadLocalObjectCollector {
	protected final ILinkedMap<Class<?>, ICollectableController> typeToCollectableControllerMap =
			new LinkedHashMap<>();

	protected int mappingVersion;

	protected final Lock lock = new ReentrantLock();

	public void clearThreadLocal() {
		SingleThreadCollector stc = get();
		if (stc == null) {
			return;
		}
		set(null);
		stc.dispose();
	}

	public void clearThreadLocals() {
		// Intended blank
	}

	protected void updateMapping(SingleThreadCollector objectCollector) {
		objectCollector.clearCollectableControllers();
		Lock lock = this.lock;
		lock.lock();
		try {
			for (Entry<Class<?>, ICollectableController> entry : typeToCollectableControllerMap) {
				objectCollector.registerCollectableController(entry.getValue(), entry.getKey());
			}
		}
		finally {
			lock.unlock();
		}
		objectCollector.setMappingVersion(mappingVersion);
	}

	@Override
	public IThreadLocalObjectCollector getCurrent() {
		return this;
	}

	public SingleThreadCollector getCurrentIntern() {
		SingleThreadCollector current = get();

		if (current == null) {
			current = new SingleThreadCollector(this);

			updateMapping(current);
			set(current);
		}
		if (mappingVersion != current.getMappingVersion()) {
			updateMapping(current);
		}
		return current;
	}

	@Override
	public <T> T create(final Class<T> myClass) {
		return getCurrentIntern().create(myClass);
	}

	@Override
	public void dispose(final Object object) {
		SingleThreadCollector current = get();
		if (current == null) {
			// Do not dispose anything if no collector is currently instantiated
			// This means that the object to dispose has not been created by the OC
			return;
		}
		current.dispose(object);
	}

	@Override
	public <T> void dispose(java.lang.Class<T> type, T object) {
		SingleThreadCollector current = get();
		if (current == null) {
			// Do not dispose anything if no collector is currently instantiated
			// This means that the object to dispose has not been created by the OC
			return;
		}
		current.dispose(type, object);
	}

	@Override
	public void cleanUp() {
		SingleThreadCollector current = get();
		if (current == null) {
			return;
		}
		current.cleanUp();
	}

	public void registerCollectableController(Class<?> handledType,
			ICollectableController collectableController) {
		Lock lock = this.lock;
		lock.lock();
		try {
			if (typeToCollectableControllerMap.containsKey(handledType)) {
				throw new UnsupportedOperationException(
						"CollectableController already mapped for type " + handledType);
			}
			typeToCollectableControllerMap.put(handledType, collectableController);
			mappingVersion++;
		}
		finally {
			lock.unlock();
		}
	}

	public void unregisterCollectableController(Class<?> handledType,
			ICollectableController collectableController) {
		Lock lock = this.lock;
		lock.lock();
		try {
			ICollectableController existingCollectableController =
					typeToCollectableControllerMap.get(handledType);
			if (existingCollectableController == null) {
				throw new UnsupportedOperationException(
						"No collectableController already mapped for type " + handledType);
			}
			if (existingCollectableController != collectableController) {
				throw new UnsupportedOperationException(
						"Given collectableController is not mapped for type " + handledType);
			}
			typeToCollectableControllerMap.remove(handledType);
			mappingVersion++;
		}
		finally {
			lock.unlock();
		}
	}
}
