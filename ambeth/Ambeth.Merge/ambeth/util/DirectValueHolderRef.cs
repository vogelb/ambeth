using De.Osthus.Ambeth.Metadata;
using De.Osthus.Ambeth.Proxy;
using System;

namespace De.Osthus.Ambeth.Util
{
    public class DirectValueHolderRef
    {
        protected readonly IObjRefContainer vhc;

        protected readonly RelationMember member;

        protected readonly bool objRefsOnly;

        public DirectValueHolderRef(IObjRefContainer vhc, RelationMember member)
        {
            this.vhc = vhc;
            this.member = member;
            this.objRefsOnly = false;
        }

        public DirectValueHolderRef(IObjRefContainer vhc, RelationMember member, bool objRefsOnly)
        {
            this.vhc = vhc;
            this.member = member;
            this.objRefsOnly = objRefsOnly;
        }

        public IObjRefContainer Vhc
        {
            get
            {
                return vhc;
            }
        }

        public RelationMember Member
        {
            get
            {
                return member;
            }
        }

        public bool ObjRefsOnly
        {
            get
            {
                return objRefsOnly;
            }
        }

        public override bool Equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }
            if (!(obj is DirectValueHolderRef))
            {
                return false;
            }
            DirectValueHolderRef other = (DirectValueHolderRef)obj;
            return Object.ReferenceEquals(Vhc, other.Vhc) && Object.ReferenceEquals(Member, other.Member);
        }

        public override int GetHashCode()
        {
            if (Member == null)
            {
                return Vhc.GetHashCode();
            }
            return Vhc.GetHashCode() ^ Member.GetHashCode();
        }
    }
}