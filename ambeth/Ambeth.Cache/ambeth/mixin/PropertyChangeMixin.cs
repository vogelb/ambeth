﻿using De.Osthus.Ambeth.Annotation;
using De.Osthus.Ambeth.Cache;
using De.Osthus.Ambeth.Cache.Config;
using De.Osthus.Ambeth.Collections;
using De.Osthus.Ambeth.Collections.Specialized;
using De.Osthus.Ambeth.Config;
using De.Osthus.Ambeth.Databinding;
using De.Osthus.Ambeth.Event;
using De.Osthus.Ambeth.Exceptions;
using De.Osthus.Ambeth.Ioc;
using De.Osthus.Ambeth.Ioc.Annotation;
using De.Osthus.Ambeth.Ioc.Extendable;
using De.Osthus.Ambeth.Log;
using De.Osthus.Ambeth.Merge.Model;
using De.Osthus.Ambeth.Metadata;
using De.Osthus.Ambeth.Model;
using De.Osthus.Ambeth.Proxy;
using De.Osthus.Ambeth.Threading;
using De.Osthus.Ambeth.Typeinfo;
using De.Osthus.Ambeth.Util;
using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using System.Reflection;
using System.Text;

namespace De.Osthus.Ambeth.Mixin
{
    public class PropertyChangeMixin : IPropertyChangeExtensionExtendable, ICollectionChangeExtensionExtendable
    {
        public static readonly Object UNKNOWN_VALUE = new Object();

        public class PropertyEntry
        {
            public readonly String propertyName;

            public readonly MemberGetDelegate getDelegate;

            public readonly MemberSetDelegate setDelegate;

            public readonly bool doesModifyToBeUpdated;

            public readonly bool firesToBeCreatedPCE;

            public readonly String[] propertyNames;

            public readonly Object[] unknownValues;

            public readonly PropertyChangedEventArgs[] pceArgs;

            public readonly bool isParentChildSetter;

            public readonly bool isAddedRemovedCheckNecessary;

            public PropertyEntry(Type type, String propertyName)
            {
                this.propertyName = propertyName;
                LinkedHashSet<String> propertyNames = new LinkedHashSet<String>();
                propertyNames.Add(propertyName);
                PropertyInfo prop = type.GetProperty(propertyName);
                doesModifyToBeUpdated = !AnnotationUtil.IsAnnotationPresent<IgnoreToBeUpdated>(prop, false);
                isParentChildSetter = AnnotationUtil.IsAnnotationPresent<ParentChild>(prop, false);
                isAddedRemovedCheckNecessary = !prop.PropertyType.IsPrimitive && ImmutableTypeSet.GetUnwrappedType(prop.PropertyType) == null
                    && !typeof(String).Equals(prop.PropertyType) && !prop.PropertyType.IsValueType;

                EvaluateDependentProperties(type, prop, propertyNames);

                while (true)
                {
                    int startCount = propertyNames.Count;

                    foreach (String currPropertyName in new List<String>(propertyNames))
                    {
                        PropertyInfo currProp = type.GetProperty(currPropertyName);
                        if (currProp.CanWrite)
                        {
                            continue;
                        }
                        // Is is just an evaluating property which has to be re-evaluated because of the change on the current property
                        EvaluateDependentProperties(type, currProp, propertyNames);
                    }
                    if (startCount == propertyNames.Count)
                    {
                        break;
                    }
                }
                this.propertyNames = propertyNames.ToArray();
                bool firesToBeCreatedPCE = false;
                unknownValues = CreateArrayOfValues(UNKNOWN_VALUE, this.propertyNames.Length);
                pceArgs = new PropertyChangedEventArgs[propertyNames.Count];
                int index = 0;
                foreach (String invokedPropertyName in propertyNames)
                {
                    pceArgs[index] = new PropertyChangedEventArgs(invokedPropertyName);
                    index++;
                    firesToBeCreatedPCE |= "ToBeCreated".Equals(invokedPropertyName);
                }
                this.firesToBeCreatedPCE = firesToBeCreatedPCE;
                if (prop.CanRead)
                {
                    getDelegate = TypeUtility.GetMemberGetDelegate(type, ValueHolderIEC.GetGetterNameOfRelationPropertyWithNoInit(prop.Name), true);
                    if (getDelegate == null)
                    {
                        getDelegate = TypeUtility.GetMemberGetDelegate(type, prop.Name);
                    }
                }
                if (prop.CanWrite)
                {
                    setDelegate = TypeUtility.GetMemberSetDelegate(type, ValueHolderIEC.GetSetterNameOfRelationPropertyWithNoInit(prop.Name), true);
                    if (setDelegate == null)
                    {
                        setDelegate = TypeUtility.GetMemberSetDelegate(type, prop.Name);
                    }
                }
            }
        }

        public static Object[] CreateArrayOfValues(Object value, int length)
        {
            Object[] values = new Object[length];
            for (int a = values.Length; a-- > 0; )
            {
                values[a] = UNKNOWN_VALUE;
            }
            values[0] = value;
            return values;
        }

        protected static void EvaluateDependentProperties(Type type, PropertyInfo pi, ICollection<String> propertyNames)
        {
            Object[] pcAttrs = pi.GetCustomAttributes(typeof(FireTargetOnPropertyChange), true);
            foreach (FireTargetOnPropertyChange pcAttr in pcAttrs)
            {
                String attrPropName = pcAttr.PropertyName;
                if (!propertyNames.Contains(attrPropName))
                {
                    propertyNames.Add(attrPropName);
                }
            }
            String propertyName = pi.Name;
            foreach (PropertyInfo currProp in type.GetProperties())
            {
                Object[] attrs = currProp.GetCustomAttributes(typeof(FireThisOnPropertyChange), true);
                String attrPropName = currProp.Name;
                foreach (FireThisOnPropertyChange ftopc in attrs)
                {
                    if (propertyName.Equals(ftopc.PropertyName))
                    {
                        if (!propertyNames.Contains(attrPropName))
                        {
                            propertyNames.Add(attrPropName);
                        }
                    }
                }
            }
        }

        [LogInstance]
        public ILogger Log { private get; set; }

        protected readonly SmartCopyMap<IPropertyInfo, PropertyEntry> propertyToEntryMap = new SmartCopyMap<IPropertyInfo, PropertyEntry>();

        protected readonly ClassExtendableListContainer<IPropertyChangeExtension> propertyChangeExtensions = new ClassExtendableListContainer<IPropertyChangeExtension>(
            "propertyChangeExtension", "entityType");

        protected readonly ClassExtendableListContainer<ICollectionChangeExtension> collectionChangeExtensions = new ClassExtendableListContainer<ICollectionChangeExtension>(
            "collectionChangeExtension", "entityType");

        [Autowired]
        public ICacheModification CacheModification { protected get; set; }

        [Autowired]
        public IGuiThreadHelper GuiThreadHelper { protected get; set; }

        [Autowired]
        public IPropertyInfoProvider PropertyInfoProvider { protected get; set; }

        [Property(CacheConfigurationConstants.AsyncPropertyChangeActive, DefaultValue = "false")]
        public bool AsyncPropertyChangeActive { protected get; set; }

        [Property(CacheConfigurationConstants.FireOldPropertyValueActive, DefaultValue = "false")]
        public bool FireOldPropertyValueActive { protected get; set; }

        protected PropertyEntry GetPropertyEntry(Type type, IPropertyInfo property)
        {
            PropertyEntry entry = propertyToEntryMap.Get(property);
            if (entry == null)
            {
                entry = new PropertyEntry(type, property.Name);
                propertyToEntryMap.Put(property, entry);
            }
            return entry;
        }

        protected void HandleRemovedItem(INotifyPropertyChangedSource obj, Object removedItem, bool isParentChildProperty)
        {
            if (removedItem is INotifyCollectionChanged)
            {
                ((INotifyCollectionChanged)removedItem).CollectionChanged -= obj.CollectionEventHandler;
            }
            if (isParentChildProperty && removedItem is INotifyPropertyChanged)
            {
                ((INotifyPropertyChanged)removedItem).PropertyChanged -= obj.ParentChildEventHandler;
            }
        }

        protected void HandleAddedItem(INotifyPropertyChangedSource obj, Object addedItem, bool isParentChildProperty)
        {
            if (isParentChildProperty && addedItem is INotifyPropertyChanged)
            {
                ((INotifyPropertyChanged)addedItem).PropertyChanged += obj.ParentChildEventHandler;
            }
            if (addedItem is INotifyCollectionChanged)
            {
                ((INotifyCollectionChanged)addedItem).CollectionChanged += obj.CollectionEventHandler;
            }
        }

        public void FirePropertyChange(INotifyPropertyChangedSource obj, PropertyChangeSupport propertyChangeSupport, IPropertyInfo property, Object oldValue, Object currentValue)
        {
            ICacheModification cacheModification = this.CacheModification;
            PropertyEntry entry = GetPropertyEntry(obj.GetType(), property);
            if (currentValue == null)
            {
                currentValue = entry.getDelegate(obj);
            }
            try
            {
                if (entry.isAddedRemovedCheckNecessary)
                {
                    if (oldValue != null)
                    {
                        HandleRemovedItem(obj, oldValue, entry.isParentChildSetter);
                    }
                    if (currentValue != null)
                    {
                        HandleAddedItem(obj, currentValue, entry.isParentChildSetter);
                    }
                }
                String[] propertyNames = entry.propertyNames;
                Object[] oldValues;
                Object[] currentValues;

                if (FireOldPropertyValueActive)
                {
                    oldValues = CreateArrayOfValues(oldValue, propertyNames.Length);
                    currentValues = Object.ReferenceEquals(currentValue, oldValue) ? oldValues : CreateArrayOfValues(currentValue, propertyNames.Length);
                }
                else
                {
                    oldValues = entry.unknownValues;
                    currentValues = oldValues;
                }
                FirePropertyChange(obj, entry.pceArgs, propertyNames, oldValues, currentValues);
                if (entry.firesToBeCreatedPCE)
                {
                    IDataObject dObj = (IDataObject)obj;
                    if (dObj.ToBeCreated && dObj.ToBeUpdated)
                    {
                        dObj.ToBeUpdated = false;
                    }
                }
            }
            finally
            {
                if (entry.doesModifyToBeUpdated && !cacheModification.ActiveOrFlushingOrInternalUpdate)
                {
                    SetToBeUpdated(obj, true);
                }
            }
        }

        protected void SetToBeUpdated(Object obj, bool value)
        {
            if (obj is IEmbeddedType)
            {
                obj = ((IEmbeddedType)obj).Root;
            }
            if (obj is IDataObject)
            {
                IDataObject dp = (IDataObject)obj;
                if (!dp.ToBeCreated)
                {
                    dp.ToBeUpdated = value;
                }
            }
        }

        public void FirePropertyChange(INotifyPropertyChangedSource obj, PropertyChangedEventArgs[] evnts, String[] propertyNames, Object[] oldValues, Object[] currentValues)
        {
            PropertyChangeSupport propertyChangeSupport = obj.PropertyChangeSupport;
            IList<IPropertyChangeExtension> extensions = propertyChangeExtensions.GetExtensions(obj.GetType());
		    if (propertyChangeSupport == null && extensions == null && !(obj is IPropertyChangedEventHandler))
		    {
			    return;
		    }
            ICacheModification cacheModification = this.CacheModification;
            if (cacheModification.Active)
            {
                cacheModification.QueuePropertyChangeEvent(delegate()
                {
                    ExecuteFirePropertyChange(propertyChangeSupport, extensions, obj, evnts, propertyNames, oldValues, currentValues);
                });
                return;
            }
            ExecuteFirePropertyChange(propertyChangeSupport, extensions, obj, evnts, propertyNames, oldValues, currentValues);
        }

        protected void ExecuteFirePropertyChange(PropertyChangeSupport propertyChangeSupport, IList<IPropertyChangeExtension> extensions, Object obj, PropertyChangedEventArgs[] evnts, String[] propertyNames, Object[] oldValues, Object[] currentValues)
        {
            if (AsyncPropertyChangeActive)
            {
                GuiThreadHelper.InvokeInGui(delegate()
                {
                    ExecuteFirePropertyChangeIntern(propertyChangeSupport, extensions, obj, evnts, propertyNames, oldValues, currentValues);
                });
            }
            else
            {
                ExecuteFirePropertyChangeIntern(propertyChangeSupport, extensions, obj, evnts, propertyNames, oldValues, currentValues);
            }
        }

        protected void ExecuteFirePropertyChangeIntern(PropertyChangeSupport propertyChangeSupport, IList<IPropertyChangeExtension> extensions, Object obj, PropertyChangedEventArgs[] evnts, String[] propertyNames, Object[] oldValues, Object[] currentValues)
        {
            bool debugEnabled = Log.DebugEnabled;
            IPropertyChangedEventHandler pcl = (IPropertyChangedEventHandler)(obj is IPropertyChangedEventHandler ? obj : null);

            for (int a = 0, size = propertyNames.Length; a < size; a++)
            {
                String propertyName = propertyNames[a];
                if (debugEnabled)
                {
                    Log.Debug("Process PCE '" + propertyName + "' on " + obj);
                }
                Object oldValue = oldValues[a];
                Object currentValue = currentValues[a];
                if (oldValue == UNKNOWN_VALUE)
                {
                    oldValue = null;
                }
                if (currentValue == UNKNOWN_VALUE)
                {
                    currentValue = null;
                }
                if (pcl != null && (oldValue != null || currentValue != null))
                {
                    // called only in "non-technical" PCEs
                    pcl.PropertyChanged(obj, evnts[a]);
                }
                if (propertyChangeSupport != null)
                {
                    propertyChangeSupport.FirePropertyChange(obj, evnts[a], propertyName, oldValue, currentValue);
                }
                for (int b = 0, sizeB = extensions.Count; b < sizeB; b++)
                {
                    extensions[b].PropertyChanged(obj, propertyName, oldValue, currentValue);
                }
            }
        }

        public bool HasPropertyChanged(Object obj, String propertyName, int currentValue, int newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, long currentValue, long newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, short currentValue, short newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, double currentValue, double newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, float currentValue, float newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, byte currentValue, byte newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, sbyte currentValue, sbyte newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, bool currentValue, bool newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, char currentValue, char newValue)
        {
            return currentValue != newValue;
        }

        public bool HasPropertyChanged(Object obj, String propertyName, String currentValue, String newValue)
        {
            bool oldIsEmpty = false, newIsEmpty = false;
            if (currentValue == null || currentValue.Length == 0)
            {
                oldIsEmpty = true;
            }
            if (newValue == null || newValue.Length == 0)
            {
                newIsEmpty = true;
            }
            if (!oldIsEmpty && !newIsEmpty)
            {
                return !Object.Equals(currentValue, newValue);
            }
            return (oldIsEmpty != newIsEmpty);
        }

        public bool HasPropertyChangedValueType(Object obj, String propertyName, Object currentValue, Object newValue)
        {
            return !Object.Equals(currentValue, newValue);
        }

        public bool HasPropertyChangedDefault(Object obj, String propertyName, Object currentValue, Object newValue)
        {
            return !Object.ReferenceEquals(currentValue, newValue);
        }

        public void AddPropertyChangeListener(PropertyChangeSupport propertyChangeSupport, PropertyChangedEventHandler listener)
        {
            propertyChangeSupport.AddPropertyChangeListener(listener);
        }

        public void RemovePropertyChangeListener(PropertyChangeSupport propertyChangeSupport, PropertyChangedEventHandler listener)
        {
            propertyChangeSupport.RemovePropertyChangeListener(listener);
        }

        public void HandleParentChildPropertyChange(INotifyPropertyChangedSource obj, Object child, PropertyChangedEventArgs evnt)
        {
            if (CacheModification.ActiveOrFlushingOrInternalUpdate)
            {
                return;
            }
            bool oldToBeUpdated = ((IDataObject)obj).ToBeUpdated;
            if (oldToBeUpdated)
            {
                return;
            }
            obj.OnPropertyChanged("ToBeUpdated", false, true);
        }

        public void HandleCollectionChange(INotifyPropertyChangedSource obj, Object sender, NotifyCollectionChangedEventArgs evnt)
        {
            IEntityMetaData metaData = ((IEntityMetaDataHolder)obj).Get__EntityMetaData();

            Object bases = null;
		    bool parentChildProperty = false;

            RelationMember[] relationMembers = metaData.RelationMembers;
		    for (int relationIndex = relationMembers.Length; relationIndex-- > 0;)
		    {
			    Object valueDirect = ((IValueHolderContainer) obj).Get__ValueDirect(relationIndex);
			    if (!Object.ReferenceEquals(valueDirect, sender))
			    {
				    continue;
			    }
			    if (relationMembers[relationIndex].GetAnnotation(typeof(ParentChild)) != null)
			    {
				    bases = obj;
				    parentChildProperty = true;
			    }
			    else
			    {
                    bases = sender;
			    }
			    break;
		    }
		    if (bases == null)
		    {
			    foreach (PrimitiveMember primitiveMember in metaData.PrimitiveMembers)
			    {
				    Object valueDirect = primitiveMember.GetValue(obj);
                    if (!Object.ReferenceEquals(valueDirect, sender))
				    {
					    continue;
				    }
				    bases = obj;
				    parentChildProperty = true;
				    break;
			    }
		    }
		    if (bases == null)
		    {
			    throw new Exception("Must never happen");
		    }
            ICacheModification cacheModification = this.CacheModification;
            bool oldCacheModification = cacheModification.Active;
            bool cacheModificationUsed = false;
            try
            {
                switch (evnt.Action)
                {
                    case NotifyCollectionChangedAction.Add:
                    case NotifyCollectionChangedAction.Remove:
                    case NotifyCollectionChangedAction.Replace:
                        if (evnt.OldItems != null)
                        {
                            foreach (Object oldItem in evnt.OldItems)
                            {
                                HandleRemovedItem(obj, oldItem, parentChildProperty);
                            }
                        }
                        if (evnt.NewItems != null)
                        {
                            foreach (Object newItem in evnt.NewItems)
                            {
                                HandleAddedItem(obj, newItem, parentChildProperty);
                            }
                        }
                        break;
#if !SILVERLIGHT
                    case NotifyCollectionChangedAction.Move:
                        // Nothing to do in that case
                        break;
#endif
                    case NotifyCollectionChangedAction.Reset:
                        throw new NotSupportedException("Reset is not allowed in a managed collection");
                    default:
                        throw RuntimeExceptionUtil.CreateEnumNotSupportedException(evnt.Action);
                }
                IList<ICollectionChangeExtension> extensions = collectionChangeExtensions.GetExtensions(obj.GetType());
                for (int a = 0, size = extensions.Count; a < size; a++)
                {
                    extensions[a].CollectionChanged(obj, evnt);
                }
            }
            finally
            {
                if (!oldCacheModification)
                {
                    SetToBeUpdated(obj, true);
                }
                if (cacheModificationUsed)
                {
                    cacheModification.Active = oldCacheModification;
                }
            }
        }

        public PropertyChangeSupport NewPropertyChangeSupport(Object entity)
        {
            return new PropertyChangeSupport();
        }

        public IPropertyInfo GetMethodHandle(INotifyPropertyChangedSource obj, String propertyName)
        {
            IPropertyInfo property = PropertyInfoProvider.GetProperty(obj, propertyName);
            if (property != null)
            {
                return property;
            }
            throw new Exception("Property not found: " + obj.GetType().FullName + "." + propertyName);
        }

        public void RegisterPropertyChangeExtension(IPropertyChangeExtension propertyChangeExtension, Type entityType)
        {
            propertyChangeExtensions.Register(propertyChangeExtension, entityType);
        }

        public void UnregisterPropertyChangeExtension(IPropertyChangeExtension propertyChangeExtension, Type entityType)
        {
            propertyChangeExtensions.Unregister(propertyChangeExtension, entityType);
        }

        public void RegisterCollectionChangeExtension(ICollectionChangeExtension collectionChangeExtension, Type entityType)
        {
            collectionChangeExtensions.Register(collectionChangeExtension, entityType);
        }

        public void UnregisterCollectionChangeExtension(ICollectionChangeExtension collectionChangeExtension, Type entityType)
        {
            collectionChangeExtensions.Unregister(collectionChangeExtension, entityType);
        }
    }
}