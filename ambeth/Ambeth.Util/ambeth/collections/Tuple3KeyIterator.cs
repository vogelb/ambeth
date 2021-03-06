using System;

namespace De.Osthus.Ambeth.Collections
{
    public class Tuple3KeyIterator<Key1, Key2, Key3, V> : AbstractIterator<Tuple3KeyEntry<Key1, Key2, Key3, V>>
    {
        protected Tuple3KeyEntry<Key1, Key2, Key3, V> currEntry, nextEntry;

        protected int index;

        protected readonly Tuple3KeyEntry<Key1, Key2, Key3, V>[] table;

        protected bool first = true;

        private readonly AbstractTuple3KeyHashMap<Key1, Key2, Key3, V> hashMap;

        public Tuple3KeyIterator(AbstractTuple3KeyHashMap<Key1, Key2, Key3, V> hashMap, Tuple3KeyEntry<Key1, Key2, Key3, V>[] table, bool removeAllowed) : base(removeAllowed)
        {
            this.hashMap = hashMap;
            this.table = table;
        }

        public override void Dispose()
        {
            // Intended blank
        }

        protected Tuple3KeyEntry<Key1, Key2, Key3, V> GetNextBucketFromIndex(int index)
        {
            Tuple3KeyEntry<Key1, Key2, Key3, V>[] table = this.table;
            while (index-- > 0)
            {
                Tuple3KeyEntry<Key1, Key2, Key3, V> entry = table[index];
                if (entry != null)
                {
                    this.index = index;
                    return entry;
                }
            }
            return null;
        }

        public override bool MoveNext()
        {
            if (first)
            {
                currEntry = GetNextBucketFromIndex(table.Length);
                if (currEntry == null)
                {
                    return false;
                }
                first = false;
                return true;
            }
            else if (currEntry != null)
            {
                currEntry = currEntry.GetNextEntry();
            }
            if (currEntry == null)
            {
                currEntry = GetNextBucketFromIndex(index);
            }
            return currEntry != null;
        }

        public override Tuple3KeyEntry<Key1, Key2, Key3, V> Current
        {
            get
            {
                return currEntry;
            }
        }

        public override void Remove()
        {
            if (!removeAllowed)
            {
                throw new NotSupportedException();
            }
            hashMap.Remove(currEntry.GetKey1(), currEntry.GetKey2(), currEntry.GetKey3());
            currEntry = null;
        }
    }
}