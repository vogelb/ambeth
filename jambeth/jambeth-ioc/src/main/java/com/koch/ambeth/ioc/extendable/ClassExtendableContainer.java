package com.koch.ambeth.ioc.extendable;

/*-
 * #%L
 * jambeth-ioc
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

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.IListElem;
import com.koch.ambeth.util.collections.IdentityHashMap;
import com.koch.ambeth.util.collections.InterfaceFastList;
import com.koch.ambeth.util.collections.LinkedHashMap;

public class ClassExtendableContainer<V> extends MapExtendableContainer<Class<?>, V> {
	public static int getDistanceForType(Class<?> existingRequestedType, Class<?> type) {
		// If a converter handles A (strong registration)
		// It implicitily handles X extends A (weak registration)
		if (existingRequestedType == null || !type.isAssignableFrom(existingRequestedType)) {
			return NO_VALID_DISTANCE;
		}
		if (existingRequestedType.equals(type)) {
			// Type matched exactly - 'strong' registration
			return 0;
		}
		if (type.equals(Object.class)) {
			return Integer.MAX_VALUE;
		}
		if (existingRequestedType.isArray() && type.isArray()) {
			// if both types are an array their distance is measured by the distance of their component
			// type
			return getDistanceForType(existingRequestedType.getComponentType(), type.getComponentType());
		}
		int bestDistance = Integer.MAX_VALUE;
		Class<?>[] currInterfaces = existingRequestedType.getInterfaces();

		for (Class<?> currInterface : currInterfaces) {
			int distance = getDistanceForType(currInterface, type);
			if (distance < 0) {
				continue;
			}
			distance += 10000;
			if (distance < bestDistance) {
				bestDistance = distance;
			}
		}
		Class<?> baseType = existingRequestedType.getSuperclass();
		if (baseType == null) {
			baseType = Object.class;
		}
		int distance = getDistanceForType(baseType, type);
		if (distance >= 0) {
			distance++;
			if (distance < bestDistance) {
				bestDistance = distance;
			}
		}
		if (bestDistance == Integer.MAX_VALUE) {
			throw new IllegalStateException("Must never happen");
		}
		return bestDistance;
	}

	public static final int NO_VALID_DISTANCE = -1;

	protected static final Object alreadyHandled = new Object();

	protected volatile ClassEntry<V> classEntry = new ClassEntry<>();

	public ClassExtendableContainer(String message, String keyMessage) {
		this(message, keyMessage, false);
	}

	public ClassExtendableContainer(String message, String keyMessage, boolean multiValue) {
		super(message, keyMessage, multiValue);
	}

	@Override
	public void clear() {
		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			super.clear();
			clearWeakCache();
		}
		finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public void clearWeakCache() {
		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			ClassExtendableContainer<V> tempCC = new ClassExtendableContainer<>("", "");
			for (Entry<Class<?>, Object> entry : this) {
				tempCC.register((V) entry.getValue(), entry.getKey());
			}
			this.classEntry = tempCC.classEntry;
		}
		finally {
			writeLock.unlock();
		}
	}

	public V getExtensionHardKey(Class<?> key) {
		return super.getExtension(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getExtension(Class<?> key) {
		if (key == null) {
			return null;
		}
		Object extension = classEntry.get(key);
		if (extension == null) {
			Lock writeLock = getWriteLock();
			writeLock.lock();
			try {
				extension = classEntry.get(key);
				if (extension == null) {
					ClassEntry<V> classEntry = copyStructure();

					classEntry.put(key, alreadyHandled);
					classEntry.typeToDefEntryMap.put(key, alreadyHandled);
					checkToWeakRegisterExistingExtensions(key, classEntry);
					this.classEntry = classEntry;

					extension = classEntry.get(key);
					if (extension == null) {
						return null;
					}
				}
			}
			finally {
				writeLock.unlock();
			}
		}
		if (extension == alreadyHandled) {
			// Already tried
			return null;
		}
		return (V) extension;
	}

	@SuppressWarnings("unchecked")
	protected ClassEntry<V> copyStructure() {
		ClassEntry<V> newClassEntry = new ClassEntry<>();
		LinkedHashMap<Class<?>, Object> newTypeToDefEntryMap = newClassEntry.typeToDefEntryMap;
		LinkedHashMap<StrongKey<V>, List<DefEntry<V>>> newDefinitionReverseMap =
				newClassEntry.definitionReverseMap;
		IdentityHashMap<DefEntry<V>, DefEntry<V>> originalToCopyMap =
				new IdentityHashMap<>();
		{
			for (Entry<Class<?>, Object> entry : classEntry.typeToDefEntryMap) {
				Class<?> key = entry.getKey();
				Object value = entry.getValue();

				if (value == alreadyHandled) {
					newTypeToDefEntryMap.put(key, alreadyHandled);
				}
				else {
					InterfaceFastList<DefEntry<V>> list = (InterfaceFastList<DefEntry<V>>) value;

					InterfaceFastList<DefEntry<V>> newList = new InterfaceFastList<>();

					IListElem<DefEntry<V>> pointer = list.first();
					while (pointer != null) {
						DefEntry<V> defEntry = pointer.getElemValue();
						DefEntry<V> newDefEntry =
								new DefEntry<>(defEntry.extension, defEntry.type, defEntry.distance);
						originalToCopyMap.put(defEntry, newDefEntry);

						newList.pushLast(newDefEntry);
						pointer = pointer.getNext();
					}
					newTypeToDefEntryMap.put(key, newList);
				}
				typeToDefEntryMapChanged(newClassEntry, key);
			}
		}
		for (Entry<StrongKey<V>, List<DefEntry<V>>> entry : classEntry.definitionReverseMap) {
			List<DefEntry<V>> defEntries = entry.getValue();
			ArrayList<DefEntry<V>> newDefEntries = new ArrayList<>(defEntries.size());

			for (int a = 0, size = defEntries.size(); a < size; a++) {
				DefEntry<V> newDefEntry = originalToCopyMap.get(defEntries.get(a));
				if (newDefEntry == null) {
					throw new IllegalStateException("Must never happen");
				}
				newDefEntries.add(newDefEntry);
			}
			newDefinitionReverseMap.put(entry.getKey(), newDefEntries);
		}
		return newClassEntry;
	}

	protected boolean checkToWeakRegisterExistingExtensions(Class<?> type, ClassEntry<V> classEntry) {
		boolean changesHappened = false;
		for (Entry<StrongKey<V>, List<DefEntry<V>>> entry : classEntry.definitionReverseMap) {
			StrongKey<V> strongKey = entry.getKey();
			Class<?> registeredStrongType = strongKey.strongType;
			int distance = getDistanceForType(type, registeredStrongType);
			if (distance == NO_VALID_DISTANCE) {
				continue;
			}
			List<DefEntry<V>> defEntries = entry.getValue();
			for (int a = 0, size = defEntries.size(); a < size; a++) {
				DefEntry<V> defEntry = defEntries.get(a);
				changesHappened |= appendRegistration(registeredStrongType, type, defEntry.extension,
						distance, classEntry);
			}
		}
		return changesHappened;
	}

	protected boolean checkToWeakRegisterExistingTypes(Class<?> type, V extension,
			ClassEntry<V> classEntry) {
		boolean changesHappened = false;
		for (Entry<Class<?>, Object> entry : classEntry.typeToDefEntryMap) {
			Class<?> existingRequestedType = entry.getKey();
			int priorityForExistingRequestedType = getDistanceForType(existingRequestedType, type);
			if (priorityForExistingRequestedType == NO_VALID_DISTANCE) {
				continue;
			}
			changesHappened |= appendRegistration(type, existingRequestedType, extension,
					priorityForExistingRequestedType, classEntry);
		}
		return changesHappened;
	}

	@Override
	public void register(V extension, Class<?> key) {
		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			super.register(extension, key);

			ClassEntry<V> classEntry = copyStructure();
			appendRegistration(key, key, extension, 0, classEntry);
			checkToWeakRegisterExistingTypes(key, extension, classEntry);
			checkToWeakRegisterExistingExtensions(key, classEntry);
			this.classEntry = classEntry;
		}
		finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void unregister(V extension, Class<?> key) {
		ParamChecker.assertParamNotNull(extension, "extension");
		ParamChecker.assertParamNotNull(key, "key");

		Lock writeLock = getWriteLock();
		writeLock.lock();
		try {
			super.unregister(extension, key);

			ClassEntry<V> classEntry = copyStructure();
			LinkedHashMap<StrongKey<V>, List<DefEntry<V>>> definitionReverseMap =
					classEntry.definitionReverseMap;
			List<DefEntry<V>> weakEntriesOfStrongType =
					definitionReverseMap.remove(new StrongKey<>(extension, key));
			if (weakEntriesOfStrongType == null) {
				return;
			}
			LinkedHashMap<Class<?>, Object> typeToDefEntryMap = classEntry.typeToDefEntryMap;
			for (int a = weakEntriesOfStrongType.size(); a-- > 0;) {
				DefEntry<V> defEntry = weakEntriesOfStrongType.get(a);
				Class<?> registeredType = defEntry.type;

				Object value = typeToDefEntryMap.get(registeredType);
				InterfaceFastList<DefEntry<V>> list = (InterfaceFastList<DefEntry<V>>) value;
				list.remove(defEntry);
				if (list.isEmpty()) {
					typeToDefEntryMap.remove(registeredType);
				}
				typeToDefEntryMapChanged(classEntry, registeredType);
			}
			this.classEntry = classEntry;
		}
		finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	protected void typeToDefEntryMapChanged(ClassEntry<V> classEntry, Class<?> key) {
		Object obj = classEntry.typeToDefEntryMap.get(key);
		if (obj == null) {
			classEntry.remove(key);
			return;
		}
		if (obj == alreadyHandled) {
			classEntry.put(key, alreadyHandled);
			return;
		}
		if (obj instanceof DefEntry) {
			classEntry.put(key, ((DefEntry<V>) obj).extension);
			return;
		}
		DefEntry<V> firstDefEntry = ((InterfaceFastList<DefEntry<V>>) obj).first().getElemValue();
		classEntry.put(key, firstDefEntry.extension);
	}

	@SuppressWarnings("unchecked")
	protected boolean appendRegistration(Class<?> strongTypeKey, Class<?> key, V extension,
			int distance, ClassEntry<V> classEntry) {
		LinkedHashMap<Class<?>, Object> typeToDefEntryMap = classEntry.typeToDefEntryMap;
		Object fastList = typeToDefEntryMap.get(key);
		if (fastList != null && fastList != alreadyHandled) {
			IListElem<DefEntry<V>> pointer = ((InterfaceFastList<DefEntry<V>>) fastList).first();
			while (pointer != null) {
				DefEntry<V> existingDefEntry = pointer.getElemValue();
				if (existingDefEntry.extension == extension && existingDefEntry.distance == distance) {
					// DefEntry already exists with same distance
					return false;
				}
				pointer = pointer.getNext();
			}
		}
		if (fastList == null || fastList == alreadyHandled) {
			fastList = new InterfaceFastList<DefEntry<V>>();
			typeToDefEntryMap.put(key, fastList);
		}
		DefEntry<V> defEntry = new DefEntry<>(extension, key, distance);

		LinkedHashMap<StrongKey<V>, List<DefEntry<V>>> definitionReverseMap =
				classEntry.definitionReverseMap;
		StrongKey<V> strongKey = new StrongKey<>(extension, strongTypeKey);
		List<DefEntry<V>> typeEntries = definitionReverseMap.get(strongKey);
		if (typeEntries == null) {
			typeEntries = new ArrayList<>();
			definitionReverseMap.put(strongKey, typeEntries);
		}
		typeEntries.add(defEntry);

		InterfaceFastList.insertOrdered((InterfaceFastList<DefEntry<V>>) fastList, defEntry);
		typeToDefEntryMapChanged(classEntry, key);
		return true;
	}
}
