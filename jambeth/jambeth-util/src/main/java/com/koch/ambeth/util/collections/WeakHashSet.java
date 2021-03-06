package com.koch.ambeth.util.collections;

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

import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.Iterator;

public class WeakHashSet<K> extends AbstractHashSet<K> {
	public static <K> WeakHashSet<K> create(int size) {
		return create(size, DEFAULT_LOAD_FACTOR);
	}

	public static <K> WeakHashSet<K> create(int size, float loadFactor) {
		return new WeakHashSet<>((int) (size / loadFactor) + 1, loadFactor);
	}

	protected int size;

	protected final ReferenceQueue<K> referenceQueue = new ReferenceQueue<>();

	public WeakHashSet() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, WeakSetEntry.class);
	}

	public WeakHashSet(Collection<? extends K> sourceCollection) {
		this((int) (sourceCollection.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR,
				WeakSetEntry.class);
		addAll(sourceCollection);
	}

	public WeakHashSet(K[] sourceArray) {
		this((int) (sourceArray.length / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR,
				WeakSetEntry.class);
		addAll(sourceArray);
	}

	public WeakHashSet(float loadFactor) {
		this(DEFAULT_INITIAL_CAPACITY, loadFactor, WeakSetEntry.class);
	}

	public WeakHashSet(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, WeakSetEntry.class);
	}

	public WeakHashSet(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, SetEntry.class);
	}

	@SuppressWarnings("rawtypes")
	public WeakHashSet(int initialCapacity, float loadFactor, Class<? extends ISetEntry> entryClass) {
		super(initialCapacity, loadFactor, entryClass);
	}

	@Override
	protected ISetEntry<K> createEntry(int hash, K key, ISetEntry<K> nextEntry) {
		return new WeakSetEntry<>(key, hash, (WeakSetEntry<K>) nextEntry, referenceQueue);
	}

	@Override
	protected void entryAdded(ISetEntry<K> entry) {
		size++;
		checkForCleanup();
	}

	@Override
	protected final void entryRemoved(ISetEntry<K> entry) {
		entryRemovedNoCleanup(entry);
		checkForCleanup();
	}

	protected void entryRemovedNoCleanup(ISetEntry<K> entry) {
		size--;
	}

	@Override
	protected boolean isEntryValid(ISetEntry<K> entry) {
		return ((WeakSetEntry<K>) entry).get() != null;
	}

	@Override
	protected void setNextEntry(ISetEntry<K> entry, ISetEntry<K> nextEntry) {
		((WeakSetEntry<K>) entry).setNextEntry((WeakSetEntry<K>) nextEntry);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public IList<K> toList() {
		IList<K> list = new ArrayList<>(size());
		toList(list);
		return list;
	}

	@Override
	public void toList(Collection<K> targetList) {
		Iterator<K> iter = iterator();
		while (iter.hasNext()) {
			K key = iter.next();
			if (key == null) {
				continue;
			}
			targetList.add(key);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] array) {
		Iterator<K> iter = iterator();
		int index = 0;
		while (iter.hasNext()) {
			K key = iter.next();
			if (key == null) {
				continue;
			}
			array[index++] = (T) key;
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	public void checkForCleanup() {
		ISetEntry<K>[] table = this.table;
		int tableLengthMinusOne = table.length - 1;
		WeakSetEntry<K> removedEntry;
		ReferenceQueue<K> referenceQueue = this.referenceQueue;
		while ((removedEntry = (WeakSetEntry<K>) referenceQueue.poll()) != null) {
			int i = removedEntry.hash & tableLengthMinusOne;
			ISetEntry<K> entry = table[i];
			if (entry == null) {
				// Nothing to do
				continue;
			}
			if (entry == removedEntry) {
				table[i] = entry.getNextEntry();
				entryRemovedNoCleanup(entry);
				continue;
			}
			ISetEntry<K> prevEntry = entry;
			entry = entry.getNextEntry();
			while (entry != null) {
				if (entry == removedEntry) {
					setNextEntry(prevEntry, entry.getNextEntry());
					entryRemovedNoCleanup(entry);
					break;
				}
				prevEntry = entry;
				entry = entry.getNextEntry();
			}
		}
	}
}
