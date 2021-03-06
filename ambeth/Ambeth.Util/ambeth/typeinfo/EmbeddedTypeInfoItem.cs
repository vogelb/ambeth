using System;
using System.Text;

namespace De.Osthus.Ambeth.Typeinfo
{
    public class EmbeddedTypeInfoItem : IEmbeddedTypeInfoItem
    {
        protected ITypeInfoItem childMember;

        protected ITypeInfoItem[] memberPath;

        protected String name;

        public EmbeddedTypeInfoItem(String name, ITypeInfoItem childMember, params ITypeInfoItem[] memberPath)
        {
            this.name = name;
            this.childMember = childMember;
            this.memberPath = memberPath;
        }

        public ITypeInfoItem[] MemberPath
        {
            get
            {
                return memberPath;
            }
        }

        public String MemberPathString
        {
            get
            {
                StringBuilder sb = new StringBuilder();
                foreach (ITypeInfoItem member in MemberPath)
                {
                    if (sb.Length > 0)
                    {
                        sb.Append('.');
                    }
                    sb.Append(member.Name);
                }
                return sb.ToString();
            }
        }

        public String[] MemberPathToken
        {
            get
            {
                ITypeInfoItem[] memberPath = MemberPath;
                String[] token = new String[memberPath.Length];
                for (int a = memberPath.Length; a-- > 0; )
                {
                    ITypeInfoItem member = memberPath[a];
                    token[a] = member.Name;
                }
                return token;
            }
        }

        public ITypeInfoItem ChildMember
        {
            get
            {
                return childMember;
            }
        }

        public Type DeclaringType
        {
            get
            {
                return childMember.DeclaringType;
            }
        }

        public Object DefaultValue
        {
            get
            {
                return childMember.DefaultValue;
            }
            set
            {
                childMember.DefaultValue = value;
            }
        }

        public Object NullEquivalentValue
        {
            get
            {
                return childMember.NullEquivalentValue;
            }
            set
            {
                childMember.NullEquivalentValue = value;
            }
        }

        public Type RealType
        {
            get
            {
                return childMember.RealType;
            }
        }

        public Type ElementType
        {
            get
            {
                return childMember.ElementType;
            }
        }

        public bool TechnicalMember
        {
            get
            {
                return childMember.TechnicalMember;
            }
            set
            {
                childMember.TechnicalMember = value;
            }
        }

        public Object GetValue(Object obj)
        {
            return GetValue(obj, false);
        }

        public Object GetValue(Object obj, bool allowNullEquivalentValue)
        {
            Object currentObj = obj;
            for (int a = 0, size = memberPath.Length; a < size; a++)
            {
                ITypeInfoItem memberPathItem = memberPath[a];
                currentObj = memberPathItem.GetValue(currentObj, allowNullEquivalentValue);
                if (currentObj == null)
                {
                    if (allowNullEquivalentValue)
                    {
                        return childMember.NullEquivalentValue;
                    }
                    return null;
                }
            }
            return childMember.GetValue(currentObj, allowNullEquivalentValue);
        }

        public void SetValue(Object obj, Object value)
        {
            Object currentObj = obj;
            for (int a = 0, size = memberPath.Length; a < size; a++)
            {
                ITypeInfoItem memberPathItem = memberPath[a];
                Object childObj = memberPathItem.GetValue(currentObj, false);
                if (childObj == null)
                {
                    childObj = Activator.CreateInstance(memberPathItem.RealType);
                    memberPathItem.SetValue(currentObj, childObj);
                }
                currentObj = childObj;
            }
            childMember.SetValue(currentObj, value);
        }

        public V GetAnnotation<V>() where V : Attribute
        {
            return childMember.GetAnnotation<V>();
        }

        public bool CanRead
        {
            get
            {
                return childMember.CanRead;
            }
        }

        public bool CanWrite
        {
            get
            {
                return childMember.CanWrite;
            }
        }

        public bool IsTechnicalMember
        {
            get
            {
                return childMember.TechnicalMember;
            }
            set
            {
                childMember.TechnicalMember = value;
            }
        }

        public String Name
        {
            get
            {
                return name;
            }
        }

        public String XMLName
        {
            get
            {
                return childMember.XMLName;
            }
        }

        public bool IsXMLIgnore
        {
            get
            {
                return childMember.IsXMLIgnore;
            }
        }

        public override String ToString()
        {
            return "Embedded: " + Name + "/" + XMLName + " " + childMember;
        }
    }
}
