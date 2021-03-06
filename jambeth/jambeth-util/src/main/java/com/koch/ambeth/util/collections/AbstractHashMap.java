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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.koch.ambeth.util.IPrintable;
import com.koch.ambeth.util.StringBuilderUtil;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;

/**
 * Abstrakte HashMap als Basisklasse fuer verschiedene spezialisierte Anwendungsfaelle
 *
 * @author kochd
 * @param <E> Typ der Entrys der Map
 * @param <K> Typ der Keys
 * @param <V> Typ der Values
 */
public abstract class AbstractHashMap<WrappedK, K, V> implements IMap<K, V>, IPrintable, Cloneable {
	public static final int DEFAULT_INITIAL_CAPACITY = 16;

	public static final int MAXIMUM_CAPACITY = 1 << 30;

	public static final float DEFAULT_LOAD_FACTOR = 0.75f;

	protected final float loadFactor;

	protected int threshold;

	protected IMapEntry<K, V>[] table;

	protected IResizeMapCallback resizeMapCallback;

	public AbstractHashMap(int initialCapacity, final float loadFactor, final Class<?> entryClass) {
		this.loadFactor = loadFactor;

		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		if (initialCapacity > MAXIMUM_CAPACITY) {
			initialCapacity = MAXIMUM_CAPACITY;
		}
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}

		// Find a power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < initialCapacity) {
			capacity <<= 1;
		}

		threshold = (int) (capacity * loadFactor);
		table = createTable(entryClass, capacity);

		init();
	}

	@SuppressWarnings("unchecked")
	protected IMapEntry<K, V>[] createTable(final int capacity) {
		return (IMapEntry<K, V>[]) Array.newInstance(table.getClass().getComponentType(), capacity);
	}

	@SuppressWarnings("unchecked")
	protected IMapEntry<K, V>[] createTable(final Class<?> entryClass, final int capacity) {
		return (IMapEntry<K, V>[]) Array.newInstance(entryClass, capacity);
	}

	protected void init() {

	}

	protected int extractHash(final K key) {
		return key.hashCode();
	}

	protected static int hash(int hash) {
		hash += ~(hash << 9);
		hash ^= hash >>> 14;
		hash += hash << 4;
		hash ^= hash >>> 10;
		return hash;
	}

	protected void addEntry(final int hash, final K key, final V value, final int bucketIndex) {
		IMapEntry<K, V>[] table = this.table;
		IMapEntry<K, V> e = table[bucketIndex];
		e = createEntry(hash, key, value, e);
		table[bucketIndex] = e;
		entryAdded(e);
		if (isResizeNeeded()) {
			resize(2 * table.length);
		}
	}

	public IResizeMapCallback getResizeMapCallback() {
		return resizeMapCallback;
	}

	public void setResizeMapCallback(IResizeMapCallback resizeMapCallback) {
		this.resizeMapCallback = resizeMapCallback;
	}

	protected boolean isResizeNeeded() {
		int threshold = this.threshold;
		if (resizeMapCallback == null) {
			return size() >= threshold;
		}
		if (size() < threshold) {
			return false;
		}
		// try to throw away invalid entries to solve the need for a resize
		// our criteria of "solving" the need is a invalidity ratio of at least 10%
		resizeMapCallback.resizeMapRequested(this);
		return size() >= threshold * 0.9;
	}

	protected void entryAdded(final IMapEntry<K, V> entry) {
		// Intended blank
	}

	protected void entryRemoved(final IMapEntry<K, V> entry) {
		// Intended blank
	}

	@SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		final int size = in.readInt();
		for (int a = 0; a < size; a++) {
			final Object key = in.readObject();
			final Object value = in.readObject();
			put((K) key, (V) value);
		}
	}

	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(size());

		final IMapEntry<K, V>[] table = this.table;

		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				out.writeObject(entry.getKey());
				out.writeObject(entry.getValue());
				entry = entry.getNextEntry();
			}
		}
	}

	/**
	 * Rehashes the contents of this map into a new array with a larger capacity. This method is
	 * called automatically when the number of keys in this map reaches its threshold. If current
	 * capacity is MAXIMUM_CAPACITY, this method does not resize the map, but sets threshold to
	 * Integer.MAX_VALUE. This has the effect of preventing future calls.
	 *
	 * @param newCapacity the new capacity, MUST be a power of two; must be greater than current
	 *        capacity unless current capacity is MAXIMUM_CAPACITY (in which case value is
	 *        irrelevant).
	 */
	protected void resize(final int newCapacity) {
		final IMapEntry<K, V>[] oldTable = table;
		final int oldCapacity = oldTable.length;
		if (oldCapacity == MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return;
		}

		final IMapEntry<K, V>[] newTable = createTable(newCapacity);
		transfer(newTable);
		table = newTable;
		threshold = (int) (newCapacity * loadFactor);
	}

	protected void transfer(final IMapEntry<K, V>[] newTable) {
		final int newCapacityMinus1 = newTable.length - 1;
		final IMapEntry<K, V>[] table = this.table;

		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a], next;
			while (entry != null) {
				next = entry.getNextEntry();
				if (isEntryValid(entry)) {
					int i = entry.getHash() & newCapacityMinus1;
					setNextEntry(entry, newTable[i]);
					newTable[i] = entry;
				}
				else {
					entryRemoved(entry);
				}
				entry = next;
			}
		}
	}

	@Override
	public V[] toArray(Class<V> arrayType) {
		@SuppressWarnings("unchecked")
		V[] array = (V[]) Array.newInstance(arrayType, size());
		return toArray(array);
	}

	public V[] toArray(final V[] targetArray) {
		int index = 0;
		IMapEntry<K, V>[] table = this.table;
		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				targetArray[index++] = entry.getValue();
				entry = entry.getNextEntry();
			}
		}
		return targetArray;
	}

	protected boolean isEntryValid(IMapEntry<K, V> entry) {
		return true;
	}

	/**
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		if (isEmpty()) {
			return;
		}
		final IMapEntry<K, V>[] table = this.table;

		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			if (entry != null) {
				table[a] = null;
				while (entry != null) {
					final IMapEntry<K, V> nextEntry = entry.getNextEntry();
					entryRemoved(entry);
					entry = nextEntry;
				}
			}
		}
	}

	/**
	 * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and values themselves are
	 * not cloned.
	 *
	 * @return a shallow copy of this map
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		AbstractHashMap<WrappedK, K, V> result = null;
		try {
			result = (AbstractHashMap<WrappedK, K, V>) super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		for (Entry<K, V> entry : this) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(final Object key) {
		final K realKey = (K) key;
		final int hash = hash(extractHash(realKey));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);
		IMapEntry<K, V> entry = table[i];

		while (entry != null) {
			if (equalKeys(realKey, entry)) {
				return true;
			}
			entry = entry.getNextEntry();
		}
		return false;
	}

	/**
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(final Object value) {
		IMapEntry<K, V>[] table = this.table;
		if (value == null) {
			for (int a = table.length; a-- > 0;) {
				IMapEntry<K, V> entry = table[a];
				while (entry != null) {
					final Object entryValue = entry.getValue();
					if (entryValue == null) {
						return true;
					}
					entry = entry.getNextEntry();
				}
			}
		}
		else {
			for (int a = table.length; a-- > 0;) {
				IMapEntry<K, V> entry = table[a];
				while (entry != null) {
					final Object entryValue = entry.getValue();
					if (value.equals(entryValue)) {
						return true;
					}
					entry = entry.getNextEntry();
				}
			}
		}
		return false;
	}

	protected boolean equalKeys(final K key, final IMapEntry<K, V> entry) {
		return key.equals(entry.getKey());
	}

	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(final K key, final V value) {
		final int hash = hash(extractHash(key));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);

		IMapEntry<K, V> entry = table[i];
		while (entry != null) {
			if (equalKeys(key, entry)) {
				if (isSetValueForEntryAllowed()) {
					return setValueForEntry(entry, value);
				}
				V oldValue = entry.getValue();
				removeEntryForKey(key);
				addEntry(hash, key, value, i);
				return oldValue;
			}
			entry = entry.getNextEntry();
		}
		addEntry(hash, key, value, i);
		return null;
	}

	@Override
	public boolean putIfNotExists(final K key, final V value) {
		final int hash = hash(extractHash(key));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);

		IMapEntry<K, V> entry = table[i];
		while (entry != null) {
			if (equalKeys(key, entry)) {
				return false;
			}
			entry = entry.getNextEntry();
		}
		addEntry(hash, key, value, i);
		return true;
	}

	@Override
	public boolean removeIfValue(final K key, final V value) {
		final int hash = hash(extractHash(key));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);
		IMapEntry<K, V> entry = table[i];
		if (entry != null) {
			if (equalKeys(key, entry)) {
				table[i] = entry.getNextEntry();
				final V existingValue = entry.getValue();
				if (existingValue != value) // Test if reference identical
				{
					return false;
				}
				entryRemoved(entry);
				return true;
			}
			IMapEntry<K, V> prevEntry = entry;
			entry = entry.getNextEntry();
			while (entry != null) {
				if (equalKeys(key, entry)) {
					setNextEntry(prevEntry, entry.getNextEntry());
					final V existingValue = entry.getValue();
					if (existingValue != value) // Test if reference identical
					{
						return false;
					}
					entryRemoved(entry);
					return true;
				}
				prevEntry = entry;
				entry = entry.getNextEntry();
			}
		}
		return false;
	}

	/**
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public V remove(final Object key) {
		return removeEntryForKey((K) key);
	}

	protected final V removeEntryForKey(final K key) {
		final int hash = hash(extractHash(key));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);
		IMapEntry<K, V> entry = table[i];
		if (entry != null) {
			if (equalKeys(key, entry)) {
				table[i] = entry.getNextEntry();
				final V value = entry.getValue();
				entryRemoved(entry);
				return value;
			}
			IMapEntry<K, V> prevEntry = entry;
			entry = entry.getNextEntry();
			while (entry != null) {
				if (equalKeys(key, entry)) {
					setNextEntry(prevEntry, entry.getNextEntry());
					final V value = entry.getValue();
					entryRemoved(entry);
					return value;
				}
				prevEntry = entry;
				entry = entry.getNextEntry();
			}
		}
		return null;
	}

	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public V get(final Object key) {
		final K realKey = (K) key;
		final int hash = hash(extractHash(realKey));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);
		IMapEntry<K, V> entry = table[i];
		while (entry != null) {
			if (equalKeys(realKey, entry)) {
				return entry.getValue();
			}
			entry = entry.getNextEntry();
		}
		return null;
	}

	@Override
	public K getKey(K key) {
		final int hash = hash(extractHash(key));
		IMapEntry<K, V>[] table = this.table;
		final int i = hash & (table.length - 1);
		IMapEntry<K, V> entry = table[i];
		while (entry != null) {
			if (equalKeys(key, entry)) {
				return entry.getKey();
			}
			entry = entry.getNextEntry();
		}
		return null;
	}

	protected boolean isSetValueForEntryAllowed() {
		return true;
	}

	protected V setValueForEntry(final IMapEntry<K, V> entry, final V value) {
		V oldValue = entry.getValue();
		entry.setValue(value);
		return oldValue;
	}

	protected abstract void setNextEntry(final IMapEntry<K, V> entry,
			final IMapEntry<K, V> nextEntry);

	protected abstract IMapEntry<K, V> createEntry(final int hash, final K key, final V value,
			final IMapEntry<K, V> nextEntry);

	/**
	 * @see java.util.Map#size()
	 */
	@Override
	public abstract int size();

	/**
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return new MapIterator<>(this, table, true);
	}

	@Override
	public ISet<Entry<K, V>> entrySet() {
		final LinkedHashSet<Entry<K, V>> entrySet = LinkedHashSet.create(size());
		entrySet(entrySet);
		return entrySet;
	}

	@Override
	public void entrySet(ISet<Entry<K, V>> targetEntrySet) {
		IMapEntry<K, V>[] table = this.table;
		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				if (entry.isValid()) {
					targetEntrySet.add(entry);
				}
				entry = entry.getNextEntry();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		if (map instanceof IMap) {
			IMap<? extends K, ? extends V> lMap = (IMap<? extends K, ? extends V>) map;

			for (Entry<?, ?> entry : lMap) {
				put((K) entry.getKey(), (V) entry.getValue());
			}
		}
		else {
			Set<?> set = map.entrySet();
			Iterator<Entry<? extends K, ? extends V>> iter =
					(Iterator<java.util.Map.Entry<? extends K, ? extends V>>) set
							.iterator();
			while (iter.hasNext()) {
				Entry<? extends K, ? extends V> entry = iter.next();
				put(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public ISet<K> keySet() {
		LinkedHashSet<K> keySet = LinkedHashSet.create(size());
		keySet(keySet);
		return keySet;
	}

	@Override
	public void keySet(Collection<K> targetKeySet) {
		IMapEntry<K, V>[] table = this.table;
		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				if (entry.isValid()) {
					targetKeySet.add(entry.getKey());
				}
				entry = entry.getNextEntry();
			}
		}
	}

	@Override
	public IList<K> keyList() {
		ArrayList<K> keySet = new ArrayList<>(size());
		keySet(keySet);
		return keySet;
	}

	@Override
	public IList<V> values() {
		IMapEntry<K, V>[] table = this.table;
		final ArrayList<V> valueList = new ArrayList<>(size());
		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				if (entry.isValid()) {
					valueList.add(entry.getValue());
				}
				entry = entry.getNextEntry();
			}
		}
		return valueList;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	@Override
	public void toString(StringBuilder sb) {
		sb.append(size()).append(" items: [");
		boolean first = true;

		IMapEntry<K, V>[] table = this.table;
		for (int a = table.length; a-- > 0;) {
			IMapEntry<K, V> entry = table[a];
			while (entry != null) {
				if (first) {
					first = false;
				}
				else {
					sb.append(',');
				}
				StringBuilderUtil.appendPrintable(sb, entry);
				entry = entry.getNextEntry();
			}
		}
		sb.append(']');
	}
}
