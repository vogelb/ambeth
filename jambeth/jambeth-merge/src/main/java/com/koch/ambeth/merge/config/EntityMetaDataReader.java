package com.koch.ambeth.merge.config;

/*-
 * #%L
 * jambeth-merge
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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.typeinfo.MethodPropertyInfo;
import com.koch.ambeth.ioc.typeinfo.TypeInfoItemUtil;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.compositeid.CompositeIdMember;
import com.koch.ambeth.merge.compositeid.ICompositeIdFactory;
import com.koch.ambeth.merge.metadata.IIntermediateMemberTypeProvider;
import com.koch.ambeth.merge.model.EntityMetaData;
import com.koch.ambeth.merge.orm.CompositeMemberConfig;
import com.koch.ambeth.merge.orm.IEntityConfig;
import com.koch.ambeth.merge.orm.IMemberConfig;
import com.koch.ambeth.merge.orm.IOrmConfig;
import com.koch.ambeth.merge.orm.IRelationConfig;
import com.koch.ambeth.merge.orm.MemberConfig;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.metadata.EmbeddedMember;
import com.koch.ambeth.service.metadata.IEmbeddedMember;
import com.koch.ambeth.service.metadata.IPrimitiveMemberWrite;
import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.service.metadata.PrimitiveMember;
import com.koch.ambeth.service.metadata.RelationMember;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.HashMap;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.collections.IMap;
import com.koch.ambeth.util.collections.ISet;
import com.koch.ambeth.util.collections.IdentityHashSet;
import com.koch.ambeth.util.collections.IdentityLinkedSet;
import com.koch.ambeth.util.collections.LinkedHashMap;
import com.koch.ambeth.util.collections.LinkedHashSet;
import com.koch.ambeth.util.typeinfo.IPropertyInfo;
import com.koch.ambeth.util.typeinfo.IPropertyInfoProvider;
import com.koch.ambeth.util.typeinfo.IRelationProvider;

public class EntityMetaDataReader implements IEntityMetaDataReader {
	@LogInstance
	private ILogger log;

	private static final Pattern containsDot = Pattern.compile("\\.");

	@Autowired
	protected ICompositeIdFactory compositeIdFactory;

	@Autowired
	protected IIntermediateMemberTypeProvider intermediateMemberTypeProvider;

	@Autowired
	protected IPropertyInfoProvider propertyInfoProvider;

	@Autowired
	protected IRelationProvider relationProvider;

	@Override
	public void addMembers(EntityMetaData metaData, IEntityConfig entityConfig) {
		Class<?> realType = entityConfig.getRealType();

		HashSet<String> memberNamesToIgnore = new HashSet<>();
		HashSet<String> explicitBasicMemberNames = new HashSet<>();
		ArrayList<IMemberConfig> embeddedMembers = new ArrayList<>();
		HashMap<String, IMemberConfig> nameToMemberConfig = new HashMap<>();
		HashMap<String, IRelationConfig> nameToRelationConfig = new HashMap<>();
		LinkedHashMap<String, Member> nameToMemberMap = new LinkedHashMap<>();

		fillNameCollections(entityConfig, memberNamesToIgnore, explicitBasicMemberNames,
				embeddedMembers, nameToMemberConfig, nameToRelationConfig);

		LinkedHashSet<PrimitiveMember> alternateIdMembers = new LinkedHashSet<>();
		LinkedHashSet<PrimitiveMember> primitiveMembers = new LinkedHashSet<>();
		LinkedHashSet<RelationMember> relationMembers = new LinkedHashSet<>();
		LinkedHashSet<Member> notMergeRelevant = new IdentityLinkedSet<>();

		LinkedHashSet<Member> containedInAlternateIdMember = new IdentityLinkedSet<>();

		IPropertyInfo[] properties = propertyInfoProvider.getProperties(realType);

		LinkedHashMap<String, Member> explicitlyConfiguredMemberNameToMember =
				new LinkedHashMap<>();

		HashMap<String, IOrmConfig> nameToConfigMap = new HashMap<>();
		// Resolve members for all explicit configurations - both simple and composite ones, each with
		// embedded
		// functionality (dot-member-path)
		for (IMemberConfig memberConfig : entityConfig.getMemberConfigIterable()) {
			putNameToConfigMap(memberConfig, nameToConfigMap);
			if (memberConfig.isIgnore()) {
				continue;
			}
			handleMemberConfig(metaData, realType, memberConfig, explicitlyConfiguredMemberNameToMember,
					nameToMemberMap);
		}
		for (IRelationConfig relationConfig : entityConfig.getRelationConfigIterable()) {
			putNameToConfigMap(relationConfig, nameToConfigMap);
			handleRelationConfig(realType, relationConfig, explicitlyConfiguredMemberNameToMember);
		}
		putNameToConfigMap(entityConfig.getIdMemberConfig(), nameToConfigMap);
		putNameToConfigMap(entityConfig.getVersionMemberConfig(), nameToConfigMap);
		putNameToConfigMap(entityConfig.getCreatedByMemberConfig(), nameToConfigMap);
		putNameToConfigMap(entityConfig.getCreatedOnMemberConfig(), nameToConfigMap);
		putNameToConfigMap(entityConfig.getUpdatedByMemberConfig(), nameToConfigMap);
		putNameToConfigMap(entityConfig.getUpdatedOnMemberConfig(), nameToConfigMap);

		metaData.setIdMember(handleMemberConfig(metaData, realType, entityConfig.getIdMemberConfig(),
				explicitlyConfiguredMemberNameToMember, nameToMemberMap));
		metaData.setVersionMember(
				handleMemberConfig(metaData, realType, entityConfig.getVersionMemberConfig(),
						explicitlyConfiguredMemberNameToMember, nameToMemberMap));
		metaData.setCreatedByMember(
				handleMemberConfig(metaData, realType, entityConfig.getCreatedByMemberConfig(),
						explicitlyConfiguredMemberNameToMember, nameToMemberMap));
		metaData.setCreatedOnMember(
				handleMemberConfig(metaData, realType, entityConfig.getCreatedOnMemberConfig(),
						explicitlyConfiguredMemberNameToMember, nameToMemberMap));
		metaData.setUpdatedByMember(
				handleMemberConfig(metaData, realType, entityConfig.getUpdatedByMemberConfig(),
						explicitlyConfiguredMemberNameToMember, nameToMemberMap));
		metaData.setUpdatedOnMember(
				handleMemberConfig(metaData, realType, entityConfig.getUpdatedOnMemberConfig(),
						explicitlyConfiguredMemberNameToMember, nameToMemberMap));

		IdentityHashSet<Member> idMembers = new IdentityHashSet<>();
		Member idMember = metaData.getIdMember();
		if (idMember instanceof CompositeIdMember) {
			idMembers.addAll(((CompositeIdMember) idMember).getMembers());
		}
		else if (idMember != null) {
			idMembers.add(idMember);
		}

		// Handle all explicitly configured members
		for (Entry<String, Member> entry : explicitlyConfiguredMemberNameToMember) {
			String memberName = entry.getKey();
			IOrmConfig ormConfig = nameToConfigMap.get(memberName);

			Member member = entry.getValue();

			if (idMembers.contains(member)) {
				continue;
			}
			if (ormConfig.isExplicitlyNotMergeRelevant()) {
				notMergeRelevant.add(member);
			}
			if (ormConfig instanceof IRelationConfig) {
				if (!relationMembers.add((RelationMember) member)) {
					throw new IllegalStateException(
							"Member has been registered as relation multiple times: " + member.getName());
				}
				continue;
			}
			if (!(ormConfig instanceof IMemberConfig)) {
				continue;
			}
			if (((IMemberConfig) ormConfig).isAlternateId()) {
				if (!alternateIdMembers.add((PrimitiveMember) member)) {
					throw new IllegalStateException(
							"Member has been registered as alternate id multiple times: " + member.getName());
				}
				if (member instanceof CompositeIdMember) {
					Member[] containedMembers = ((CompositeIdMember) member).getMembers();
					containedInAlternateIdMember.addAll(containedMembers);
				}
			}
			if (!(member instanceof CompositeIdMember) && metaData.getVersionMember() != member) {
				// Alternate Ids are normally primitives, too. But Composite Alternate Ids not - only their
				// composite
				// items are primitives
				primitiveMembers.add((PrimitiveMember) member);
			}
		}
		IdentityHashSet<String> explicitTypeInfoItems =
				IdentityHashSet.<String>create(explicitlyConfiguredMemberNameToMember.size());
		for (Entry<String, Member> entry : explicitlyConfiguredMemberNameToMember) {
			Member member = entry.getValue();
			explicitTypeInfoItems.add(member.getName());
			if (member instanceof IEmbeddedMember) {
				explicitTypeInfoItems.add(((IEmbeddedMember) member).getMemberPath()[0].getName());
			}
		}
		// Go through the available members to look for potential auto-mapping (simple, no embedded)
		for (int i = 0; i < properties.length; i++) {
			IPropertyInfo property = properties[i];
			String memberName = property.getName();
			if (memberNamesToIgnore.contains(memberName)) {
				continue;
			}
			if (explicitTypeInfoItems.contains(memberName)) {
				// already configured, no auto mapping needed for this member
				continue;
			}

			MethodPropertyInfo mProperty = (MethodPropertyInfo) property;
			Class<?> elementType = TypeInfoItemUtil.getElementTypeUsingReflection(
					mProperty.getGetter().getReturnType(), mProperty.getGetter().getGenericReturnType());
			if (nameToMemberMap.get(property.getName()) instanceof RelationMember
					|| relationProvider.isEntityType(elementType)) {
				RelationMember member =
						getRelationMember(metaData.getEntityType(), property, nameToMemberMap);
				relationMembers.add(member);
				continue;
			}
			PrimitiveMember member =
					getPrimitiveMember(metaData.getEntityType(), property, nameToMemberMap);
			if (metaData.getIdMember() == null && memberName.equals(EntityMetaData.DEFAULT_NAME_ID)) {
				metaData.setIdMember(member);
				continue;
			}
			if (idMembers.contains(member) && !alternateIdMembers.contains(member)
					&& !containedInAlternateIdMember.contains(member)) {
				continue;
			}
			if (member.equals(metaData.getIdMember()) || member.equals(metaData.getVersionMember())
					|| member.equals(metaData.getCreatedByMember())
					|| member.equals(metaData.getCreatedOnMember())
					|| member.equals(metaData.getUpdatedByMember())
					|| member.equals(metaData.getUpdatedOnMember())) {
				continue;
			}
			if (metaData.getVersionMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_VERSION)) {
				metaData.setVersionMember(member);
				continue;
			}
			if (metaData.getCreatedByMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_CREATED_BY)) {
				metaData.setCreatedByMember(member);
			}
			else if (metaData.getCreatedOnMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_CREATED_ON)) {
				metaData.setCreatedOnMember(member);
			}
			else if (metaData.getUpdatedByMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_UPDATED_BY)) {
				metaData.setUpdatedByMember(member);
			}
			else if (metaData.getUpdatedOnMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_UPDATED_ON)) {
				metaData.setUpdatedOnMember(member);
			}
			primitiveMembers.add(member);
		}
		for (PrimitiveMember member : primitiveMembers) {
			String memberName = member.getName();
			if (explicitBasicMemberNames.contains(memberName)) {
				// Even if the name would match, this member was explicitly configured as "basic"
				continue;
			}
			if (metaData.getCreatedByMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_CREATED_BY)) {
				metaData.setCreatedByMember(member);
			}
			else if (metaData.getCreatedOnMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_CREATED_ON)) {
				metaData.setCreatedOnMember(member);
			}
			else if (metaData.getUpdatedByMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_UPDATED_BY)) {
				metaData.setUpdatedByMember(member);
			}
			else if (metaData.getUpdatedOnMember() == null
					&& memberName.equals(EntityMetaData.DEFAULT_NAME_UPDATED_ON)) {
				metaData.setUpdatedOnMember(member);
			}
		}
		filterWrongRelationMappings(relationMembers);
		// Order of setter calls is important
		PrimitiveMember[] primitives = primitiveMembers.toArray(PrimitiveMember.class);
		PrimitiveMember[] alternateIds = alternateIdMembers.toArray(PrimitiveMember.class);
		RelationMember[] relations = relationMembers.toArray(RelationMember.class);
		Arrays.sort(primitives);
		Arrays.sort(alternateIds);
		Arrays.sort(relations);
		metaData.setPrimitiveMembers(primitives);
		metaData.setAlternateIdMembers(alternateIds);
		metaData.setRelationMembers(relations);

		for (Member member : notMergeRelevant) {
			metaData.setMergeRelevant(member, false);
		}
		if (metaData.getIdMember() == null) {
			throw new IllegalStateException(
					"No ID member could be resolved for entity of type " + metaData.getRealType());
		}
	}

	protected void putNameToConfigMap(IOrmConfig config, Map<String, IOrmConfig> nameToConfigMap) {
		if (config == null) {
			return;
		}
		nameToConfigMap.put(config.getName(), config);
		if (config instanceof CompositeMemberConfig) {
			for (MemberConfig member : ((CompositeMemberConfig) config).getMembers()) {
				putNameToConfigMap(member, nameToConfigMap);
			}
		}
	}

	protected void filterWrongRelationMappings(ISet<RelationMember> relationMembers) {
		// filter all relations which can not be a relation because of explicit embedded property
		// mapping
		IdentityHashSet<RelationMember> toRemove = new IdentityHashSet<>();
		for (RelationMember relationMember : relationMembers) {
			String[] memberPath = EmbeddedMember.split(relationMember.getName());
			for (RelationMember otherRelationMember : relationMembers) {
				if (relationMember == otherRelationMember || toRemove.contains(otherRelationMember)) {
					continue;
				}
				if (!(otherRelationMember instanceof IEmbeddedMember)) {
					// only embedded members can help identifying other wrong relation members
					continue;
				}
				String[] otherMemberPath = ((IEmbeddedMember) otherRelationMember).getMemberPathToken();
				if (memberPath.length > otherMemberPath.length) {
					continue;
				}
				boolean match = true;
				for (int a = 0, size = memberPath.length; a < size; a++) {
					if (!memberPath[a].equals(otherMemberPath[a])) {
						match = false;
						break;
					}
				}
				if (match) {
					toRemove.add(relationMember);
					break;
				}
			}
		}
		relationMembers.removeAll(toRemove);
	}

	protected PrimitiveMember getPrimitiveMember(Class<?> entityType, IPropertyInfo property,
			Map<String, Member> nameToMemberMap) {
		PrimitiveMember member = (PrimitiveMember) nameToMemberMap.get(property.getName());
		if (member != null) {
			return member;
		}
		member = intermediateMemberTypeProvider.getIntermediatePrimitiveMember(entityType,
				property.getName());
		nameToMemberMap.put(property.getName(), member);
		return member;
	}

	protected RelationMember getRelationMember(Class<?> entityType, IPropertyInfo property,
			Map<String, Member> nameToMemberMap) {
		RelationMember member = (RelationMember) nameToMemberMap.get(property.getName());
		if (member != null) {
			return member;
		}
		member = intermediateMemberTypeProvider.getIntermediateRelationMember(entityType,
				property.getName());
		nameToMemberMap.put(property.getName(), member);
		return member;
	}

	protected PrimitiveMember handleMemberConfigIfNew(Class<?> entityType, String memberName,
			Map<String, Member> memberConfigToInfoItem) {
		PrimitiveMember member = (PrimitiveMember) memberConfigToInfoItem.get(memberName);
		if (member != null) {
			return member;
		}
		member = intermediateMemberTypeProvider.getIntermediatePrimitiveMember(entityType, memberName);
		if (member == null) {
			throw new RuntimeException("No member with name '" + memberName + "' found on entity type '"
					+ entityType.getName() + "'");
		}
		memberConfigToInfoItem.put(memberName, member);
		return member;
	}

	protected PrimitiveMember handleMemberConfig(IEntityMetaData metaData, Class<?> realType,
			IMemberConfig memberConfig, Map<String, Member> explicitMemberNameToMember,
			Map<String, Member> allMemberNameToMember) {
		if (memberConfig == null) {
			return null;
		}
		if (!(memberConfig instanceof CompositeMemberConfig)) {
			PrimitiveMember member =
					handleMemberConfigIfNew(realType, memberConfig.getName(), allMemberNameToMember);
			explicitMemberNameToMember.put(memberConfig.getName(), member);
			((IPrimitiveMemberWrite) member).setTransient(memberConfig.isTransient());

			PrimitiveMember definedBy = memberConfig.getDefinedBy() != null
					? handleMemberConfigIfNew(realType, memberConfig.getDefinedBy(), allMemberNameToMember)
					: null;
			((IPrimitiveMemberWrite) member).setDefinedBy(definedBy);
			return member;
		}
		MemberConfig[] memberConfigs = ((CompositeMemberConfig) memberConfig).getMembers();
		PrimitiveMember[] members = new PrimitiveMember[memberConfigs.length];
		for (int a = memberConfigs.length; a-- > 0;) {
			MemberConfig memberPart = memberConfigs[a];
			PrimitiveMember member =
					handleMemberConfigIfNew(realType, memberPart.getName(), explicitMemberNameToMember);
			members[a] = member;
		}
		PrimitiveMember compositeIdMember =
				compositeIdFactory.createCompositeIdMember(metaData, members);
		explicitMemberNameToMember.put(memberConfig.getName(), compositeIdMember);
		allMemberNameToMember.put(memberConfig.getName(), compositeIdMember);
		((IPrimitiveMemberWrite) compositeIdMember).setTransient(memberConfig.isTransient());

		PrimitiveMember definedBy = memberConfig.getDefinedBy() != null
				? handleMemberConfigIfNew(realType, memberConfig.getDefinedBy(), allMemberNameToMember)
				: null;
		((IPrimitiveMemberWrite) compositeIdMember).setDefinedBy(definedBy);
		return compositeIdMember;
	}

	protected Member handleRelationConfig(Class<?> realType, IRelationConfig relationConfig,
			Map<String, Member> relationConfigToInfoItem) {
		if (relationConfig == null) {
			return null;
		}
		Member member = relationConfigToInfoItem.get(relationConfig.getName());
		if (member != null) {
			return member;
		}
		member = intermediateMemberTypeProvider.getIntermediateRelationMember(realType,
				relationConfig.getName());
		if (member == null) {
			throw new RuntimeException("No member with name '" + relationConfig.getName()
					+ "' found on entity type '" + realType.getName() + "'");
		}
		relationConfigToInfoItem.put(relationConfig.getName(), member);
		return member;
	}

	protected void fillNameCollections(IEntityConfig entityConfig, ISet<String> memberNamesToIgnore,
			HashSet<String> explicitBasicMemberNames, IList<IMemberConfig> embeddedMembers,
			IMap<String, IMemberConfig> nameToMemberConfig,
			IMap<String, IRelationConfig> nameToRelationConfig) {
		for (IMemberConfig memberConfig : entityConfig.getMemberConfigIterable()) {
			if (!(memberConfig instanceof MemberConfig)
					&& !(memberConfig instanceof CompositeMemberConfig)) {
				throw new IllegalStateException("Member configurations of type '"
						+ memberConfig.getClass().getName() + "' not yet supported");
			}

			String memberName = memberConfig.getName();

			if (memberConfig.isIgnore()) {
				memberNamesToIgnore.add(memberName);
				memberNamesToIgnore.add(memberName + "Specified");
				continue;
			}

			explicitBasicMemberNames.add(memberName);

			String[] parts = containsDot.split(memberName, 2);
			boolean isEmbeddedMember = parts.length > 1;

			if (isEmbeddedMember) {
				embeddedMembers.add(memberConfig);
				memberNamesToIgnore.add(parts[0]);
				memberNamesToIgnore.add(parts[0] + "Specified");
				continue;
			}
			nameToMemberConfig.put(memberName, memberConfig);
		}

		for (IRelationConfig relationConfig : entityConfig.getRelationConfigIterable()) {
			String relationName = relationConfig.getName();

			nameToRelationConfig.put(relationName, relationConfig);
		}
	}
}
