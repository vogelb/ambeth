using System;
using System.Collections.Generic;

namespace De.Osthus.Ambeth.Util
{
    public interface IPrefetchHelper
    {
        IPrefetchConfig CreatePrefetch();

        IPrefetchState Prefetch(Object objects);

	    ICollection<T> ExtractTargetEntities<T, S>(IEnumerable<S> sourceEntities, String sourceToTargetEntityPropertyPath);
    }
}