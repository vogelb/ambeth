using System;
using System.Collections;
using System.Collections.Generic;

namespace De.Osthus.Ambeth.Collections
{
    public interface IISet<K> : ISet<K>, IICollection<K>
    {
        K Get(K key);

        bool ContainsAny(IEnumerable c);

		bool ContainsAny<S>(S[] array) where S : K;

        IList<K> ToList();

        K RemoveAndGet(K key);

        bool RemoveAll(IEnumerable c);
    }
}