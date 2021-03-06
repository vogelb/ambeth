using De.Osthus.Ambeth.Collections;
using De.Osthus.Ambeth.CompositeId;
using De.Osthus.Ambeth.Merge.Model;
using De.Osthus.Ambeth.Metadata;
using De.Osthus.Ambeth.Mixin;
using De.Osthus.Ambeth.Typeinfo;
using De.Osthus.Ambeth.Util;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;

namespace De.Osthus.Ambeth.Bytecode.Visitor
{
    public class InitializeEmbeddedMemberVisitor : ClassVisitor
    {
        public static readonly Type templateType = typeof(EmbeddedMemberMixin);

        public static readonly String templatePropertyName = "__" + templateType.Name;

        protected static readonly MethodInstance template_m_createEmbeddedObject = new MethodInstance(null, templateType, typeof(Object), "CreateEmbeddedObject",
                typeof(Type), typeof(Type), typeof(Object), typeof(String));

        public static PropertyInstance GetEmbeddedMemberTemplatePI(IClassVisitor cv)
        {
            Object bean = State.BeanContext.GetService(templateType);
            PropertyInstance pi = State.GetProperty(templatePropertyName, NewType.GetType(bean.GetType()));
            if (pi != null)
            {
                return pi;
            }
            return cv.ImplementAssignedReadonlyProperty(templatePropertyName, bean);
        }

        public static bool IsEmbeddedMember(IEntityMetaData metaData, String name)
        {
            String[] nameSplit = EmbeddedMember.Split(name);
            foreach (Member member in metaData.PrimitiveMembers)
            {
                if (!(member is IEmbeddedMember))
                {
                    continue;
                }
                if (((IEmbeddedMember)member).GetMemberPathToken()[0].Equals(nameSplit[0]))
                {
                    return true;
                }
            }
            foreach (RelationMember member in metaData.RelationMembers)
            {
                if (!(member is IEmbeddedMember))
                {
                    continue;
                }
                if (((IEmbeddedMember)member).GetMemberPathToken()[0].Equals(nameSplit[0]))
                {
                    return true;
                }
            }
            return false;
        }

        protected IPropertyInfoProvider propertyInfoProvider;

        protected IEntityMetaData metaData;

        protected String memberPath;

        protected String[] memberPathSplit;

        public InitializeEmbeddedMemberVisitor(IClassVisitor cv, IEntityMetaData metaData, String memberPath, IPropertyInfoProvider propertyInfoProvider)
            : base(cv)
        {
            this.metaData = metaData;
            this.memberPath = memberPath;
            this.memberPathSplit = memberPath != null ? EmbeddedMember.Split(memberPath) : null;
            this.propertyInfoProvider = propertyInfoProvider;
        }

        public override void VisitEnd()
        {
            PropertyInstance p_embeddedMemberTemplate = GetEmbeddedMemberTemplatePI(this);
            ImplementConstructor(p_embeddedMemberTemplate);

            base.VisitEnd();
        }

        protected void ImplementConstructor(PropertyInstance p_embeddedMemberTemplate)
        {
            HashSet<Member> alreadyHandledFirstMembers = new HashSet<Member>();

            List<Script> scripts = new List<Script>();

            {
                Script script = HandleMember(p_embeddedMemberTemplate, metaData.IdMember, alreadyHandledFirstMembers);
                if (script != null)
                {
                    scripts.Add(script);
                }
            }
            foreach (Member member in metaData.PrimitiveMembers)
            {
                Script script = HandleMember(p_embeddedMemberTemplate, member, alreadyHandledFirstMembers);
                if (script != null)
                {
                    scripts.Add(script);
                }
            }
            foreach (RelationMember member in metaData.RelationMembers)
            {
                Script script = HandleMember(p_embeddedMemberTemplate, member, alreadyHandledFirstMembers);
                if (script != null)
                {
                    scripts.Add(script);
                }
            }
            if (scripts.Count == 0)
            {
                return;
            }
            OverrideConstructors(delegate(IClassVisitor cv, ConstructorInstance superConstructor)
                {
                    IMethodVisitor mv = cv.VisitMethod(superConstructor);
                    mv.LoadThis();
                    mv.LoadArgs();
                    mv.InvokeSuperOfCurrentMethod();

                    foreach (Script script in scripts)
                    {
                        script(mv);
                    }
                    mv.ReturnValue();
                    mv.EndMethod();
                });
        }

        protected Script HandleMember(PropertyInstance p_embeddedMemberTemplate, Member member, ISet<Member> alreadyHandledFirstMembers)
        {
            if (member is CompositeIdMember)
		    {
			    PrimitiveMember[] members = ((CompositeIdMember) member).Members;
			    Script aggregatedScript = null;
			    for (int a = 0, size = members.Length; a < size; a++)
			    {
				    Script script = HandleMember(p_embeddedMemberTemplate, members[a], alreadyHandledFirstMembers);
				    if (script == null)
				    {
					    continue;
				    }
				    if (aggregatedScript == null)
				    {
					    aggregatedScript = script;
					    continue;
				    }
				    Script oldAggregatedScript = aggregatedScript;
				    aggregatedScript = new Script(delegate(IMethodVisitor mg)
					    {
						    oldAggregatedScript(mg);
						    script(mg);
					    });
			    }
			    return aggregatedScript;
		    }
            if (!(member is IEmbeddedMember))
            {
                return null;
            }
            Member[] memberPath = ((IEmbeddedMember)member).GetMemberPath();
            Member firstMember;
            if (memberPathSplit != null)
            {
                if (memberPath.Length < memberPathSplit.Length)
                {
                    // nothing to do in this case. This member has nothing to do with our current scope
                    return null;
                }
                for (int a = 0, size = memberPathSplit.Length; a < size; a++)
                {
                    if (!memberPathSplit[a].Equals(memberPath[a].Name))
                    {
                        // nothing to do in this case. This member has nothing to do with our current scope
                        return null;
                    }
                }
                if (memberPath.Length > memberPathSplit.Length)
                {
                    firstMember = memberPath[memberPathSplit.Length];
                }
                else
                {
                    // nothing to do in this case. This is a leaf member
                    return null;
                }
            }
            else
            {
                firstMember = memberPath[0];
            }
            if (!alreadyHandledFirstMembers.Add(firstMember))
            {
                return null;
            }
            return CreateEmbeddedObjectInstance(p_embeddedMemberTemplate, firstMember, this.memberPath != null ? this.memberPath + "." + firstMember.Name
                    : firstMember.Name);
        }

        protected Script CreateEmbeddedObjectInstance(PropertyInstance p_embeddedMemberTemplate, Member firstMember, String memberPath)
        {
            PropertyInstance property = PropertyInstance.FindByTemplate(firstMember.Name, firstMember.RealType, false);
            PropertyInstance p_rootEntity = memberPathSplit == null ? null : EmbeddedTypeVisitor.GetRootEntityProperty(this);

            return delegate(IMethodVisitor mg2)
                {
                    mg2.CallThisSetter(property, delegate(IMethodVisitor mg)
                        {
                            // Object p_embeddedMemberTemplate.createEmbeddedObject(Class<?> embeddedType, Class<?> entityType, Object parentObject, String
                            // memberPath)
                            mg.CallThisGetter(p_embeddedMemberTemplate);

                            mg.Push(firstMember.RealType); // embeddedType

                            if (p_rootEntity != null)
                            {
                                mg.CallThisGetter(p_rootEntity);
                                mg.CheckCast(EntityMetaDataHolderVisitor.m_template_getEntityMetaData.Owner);
                                mg.InvokeInterface(EntityMetaDataHolderVisitor.m_template_getEntityMetaData);
                            }
                            else
                            {
                                mg.CallThisGetter(EntityMetaDataHolderVisitor.m_template_getEntityMetaData);
                            }
                            mg.InvokeInterface(new MethodInstance(null, typeof(IEntityMetaData), typeof(Type), "get_EnhancedType"));
                            mg.LoadThis(); // parentObject
                            mg.Push(memberPath);

                            mg.InvokeVirtual(template_m_createEmbeddedObject);
                            mg.CheckCast(firstMember.RealType);
                        });
                };
        }

        protected void InvokeGetProperty(IMethodVisitor mv, IPropertyInfo property)
        {
            if (property is MethodPropertyInfo)
            {
                MethodInfo method = ((MethodPropertyInfo)property).Getter;
                mv.InvokeVirtual(new MethodInstance(method));
            }
            else
            {
                FieldInfo field = ((FieldPropertyInfo)property).BackingField;
                mv.GetField(new FieldInstance(field));
            }
        }

        protected void InvokeSetProperty(IMethodVisitor mv, IPropertyInfo property)
        {
            if (property is MethodPropertyInfo)
            {
                MethodInfo method = ((MethodPropertyInfo)property).Setter;
                mv.InvokeVirtual(new MethodInstance(method));
            }
            else
            {
                FieldInfo field = ((FieldPropertyInfo)property).BackingField;
                mv.PutField(new FieldInstance(field));
            }
        }
    }
}