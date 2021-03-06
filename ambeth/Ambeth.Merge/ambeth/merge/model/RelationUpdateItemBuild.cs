using De.Osthus.Ambeth.Collections;
using De.Osthus.Ambeth.Merge.Transfer;
using De.Osthus.Ambeth.Util;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;

namespace De.Osthus.Ambeth.Merge.Model
{
	public class RelationUpdateItemBuild : IRelationUpdateItem, IPrintable
    {
        protected String memberName;

        protected IISet<IObjRef> addedORIs = EmptySet.Empty<IObjRef>();

        protected IISet<IObjRef> removedORIs = EmptySet.Empty<IObjRef>();

        public RelationUpdateItemBuild(String memberName)
        {
            this.memberName = memberName;
        }

        public String MemberName
        {
            get
            {
                return memberName;
            }
        }

        public IObjRef[] AddedORIs
        {
            get
            {
                if (((ICollection)addedORIs).Count == 0)
                {
                    return null;
                }
                return addedORIs.ToArray();
            }
        }

        public IObjRef[] RemovedORIs
        {
            get
            {
                if (((ICollection)removedORIs).Count == 0)
                {
                    return null;
                }
                return removedORIs.ToArray();
            }
        }

        public void AddObjRef(IObjRef objRef)
        {
            if (((ICollection)addedORIs).Count == 0)
            {
                addedORIs = new CHashSet<IObjRef>();
            }
            addedORIs.Add(objRef);
        }

        public void AddObjRefs(IObjRef[] objRefs)
        {
            foreach (IObjRef objRef in objRefs)
            {
                AddObjRef(objRef);
            }
        }

        public void AddObjRefs(List<IObjRef> objRefs)
        {
            for (int a = 0, size = objRefs.Count; a < size; a++)
            {
                AddObjRef(objRefs[a]);
            }
        }

        public void AddObjRefs(IEnumerable<IObjRef> objRefs)
        {
            foreach (IObjRef objRef in objRefs)
            {
                AddObjRef(objRef);
            }
        }

        public void RemoveObjRef(IObjRef objRef)
        {
            if (((ICollection)removedORIs).Count == 0)
            {
                removedORIs = new CHashSet<IObjRef>();
            }
            removedORIs.Add(objRef);
        }

        public void RemoveObjRefs(IObjRef[] objRefs)
        {
            foreach (IObjRef objRef in objRefs)
            {
                RemoveObjRef(objRef);
            }
        }

        public void RemoveObjRefs(List<IObjRef> objRefs)
        {
            for (int a = objRefs.Count; a-- > 0; )
            {
                RemoveObjRef(objRefs[a]);
            }
        }

        public void RemoveObjRefs(IEnumerable<IObjRef> objRefs)
        {
            foreach (IObjRef objRef in objRefs)
            {
                RemoveObjRef(objRef);
            }
        }

        public IRelationUpdateItem BuildRUI()
        {
            RelationUpdateItem rui = new RelationUpdateItem();
            rui.MemberName = memberName;
            rui.AddedORIs = AddedORIs;
            rui.RemovedORIs = RemovedORIs;
            return rui;
        }

		public override String ToString()
		{
			StringBuilder sb = new StringBuilder();
			ToString(sb);
			return sb.ToString();
		}

		public void ToString(StringBuilder sb)
		{
			sb.Append("PUI: MemberName=").Append(MemberName);
			IObjRef[] addedORIs = AddedORIs;
			IObjRef[] removedORIs = RemovedORIs;
			if (addedORIs != null && addedORIs.Length > 0)
			{
				sb.Append(" AddedORIs=");
				Arrays.ToString(sb, addedORIs);
			}
			if (removedORIs != null && removedORIs.Length > 0)
			{
				sb.Append(" RemovedORIs=");
				Arrays.ToString(sb, removedORIs);
			}
		}
    }
}
