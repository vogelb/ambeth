using De.Osthus.Ambeth.Accessor;
using De.Osthus.Ambeth.Collections;
using De.Osthus.Ambeth.Typeinfo;
using System;

namespace De.Osthus.Ambeth.Metadata
{
    public abstract class Member : AbstractAccessor, IComparable<Member>
    {
        public abstract Type EntityType { get; }

        public abstract Type ElementType { get; }

        public abstract Type DeclaringType { get; }

        public abstract Type RealType { get; }

        public abstract bool IsToMany { get; }

        public abstract Object NullEquivalentValue { get; }

        public abstract Attribute GetAnnotation(Type annotationType);

        public abstract String Name { get; }
        
        public int CompareTo(Member o)
        {
            return Name.CompareTo(o.Name);
        }

	    public override bool Equals(Object obj)
	    {
		    if (obj == this)
		    {
			    return true;
		    }
		    if (obj == null || !(obj is Member))
		    {
			    return false;
		    }
		    Member other = (Member) obj;
            return EntityType.Equals(other.EntityType) && Name.Equals(other.Name);
	    }

	    public override int GetHashCode()
	    {
            return EntityType.GetHashCode() ^ Name.GetHashCode();
	    }

        public override String ToString()
        {
            return "Member " + Name;
        }
    }
}