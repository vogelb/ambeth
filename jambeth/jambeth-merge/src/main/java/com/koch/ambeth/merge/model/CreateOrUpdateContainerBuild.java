package com.koch.ambeth.merge.model;

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

import com.koch.ambeth.merge.ICUDResultHelper;
import com.koch.ambeth.merge.proxy.IEntityMetaDataHolder;
import com.koch.ambeth.merge.transfer.AbstractChangeContainer;
import com.koch.ambeth.merge.transfer.CreateContainer;
import com.koch.ambeth.merge.transfer.PrimitiveUpdateItem;
import com.koch.ambeth.merge.transfer.UpdateContainer;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.util.StringBuilderUtil;
import com.koch.ambeth.util.collections.HashMap;

public class CreateOrUpdateContainerBuild extends AbstractChangeContainer
		implements ICreateOrUpdateContainer, IEntityMetaDataHolder {
	protected IPrimitiveUpdateItem[] fullPUIs;

	protected final HashMap<String, Integer> relationNameToIndexMap;

	protected final HashMap<String, Integer> primitiveNameToIndexMap;

	protected IRelationUpdateItem[] fullRUIs;

	protected final IEntityMetaData metaData;

	protected int ruiCount, puiCount;

	protected boolean isCreate;

	protected final ICUDResultHelper cudResultHelper;

	public CreateOrUpdateContainerBuild(IEntityMetaData metaData, boolean isCreate,
			HashMap<String, Integer> relationNameToIndexMap,
			HashMap<String, Integer> primitiveNameToIndexMap, ICUDResultHelper cudResultHelper) {
		this.metaData = metaData;
		this.isCreate = isCreate;
		this.relationNameToIndexMap = relationNameToIndexMap;
		this.primitiveNameToIndexMap = primitiveNameToIndexMap;
		this.cudResultHelper = cudResultHelper;
	}

	@Override
	public IEntityMetaData get__EntityMetaData() {
		return metaData;
	}

	public boolean isCreate() {
		return isCreate;
	}

	public boolean isUpdate() {
		return !isCreate();
	}

	@Override
	public IPrimitiveUpdateItem[] getFullPUIs() {
		return fullPUIs;
	}

	@Override
	public IRelationUpdateItem[] getFullRUIs() {
		return fullRUIs;
	}

	public void addPrimitive(IPrimitiveUpdateItem pui) {
		IPrimitiveUpdateItem[] fullPUIs = this.fullPUIs;
		if (fullPUIs == null) {
			fullPUIs = new IPrimitiveUpdateItem[primitiveNameToIndexMap.size()];
			this.fullPUIs = fullPUIs;
		}
		Integer indexR = primitiveNameToIndexMap.get(pui.getMemberName());
		if (indexR == null) {
			throw new IllegalStateException("No primitive member '"
					+ getReference().getRealType().getName() + "." + pui.getMemberName() + "' defined");
		}
		int index = indexR.intValue();
		if (fullPUIs[index] == null) {
			puiCount++;
		}
		fullPUIs[index] = pui;
	}

	public void addRelation(IRelationUpdateItem rui) {
		IRelationUpdateItem[] fullRUIs = this.fullRUIs;
		if (fullRUIs == null) {
			fullRUIs = new IRelationUpdateItem[relationNameToIndexMap.size()];
			this.fullRUIs = fullRUIs;
		}
		Integer indexR = relationNameToIndexMap.get(rui.getMemberName());
		if (indexR == null) {
			throw new IllegalStateException("No relation member '"
					+ getReference().getRealType().getName() + "." + rui.getMemberName() + "' defined");
		}
		int index = indexR.intValue();
		if (fullRUIs[index] == null) {
			ruiCount++;
		}
		fullRUIs[index] = rui;
	}

	public PrimitiveUpdateItem findPrimitive(String memberName) {
		if (fullPUIs == null) {
			return null;
		}
		Integer indexR = primitiveNameToIndexMap.get(memberName);
		if (indexR == null) {
			throw new IllegalStateException("No primitive member '"
					+ getReference().getRealType().getName() + "." + memberName + "' defined");
		}
		return (PrimitiveUpdateItem) fullPUIs[indexR.intValue()];
	}

	public RelationUpdateItemBuild findRelation(String memberName) {
		if (fullRUIs == null) {
			return null;
		}
		Integer indexR = relationNameToIndexMap.get(memberName);
		if (indexR == null) {
			throw new IllegalStateException("No relational member '"
					+ getReference().getRealType().getName() + "." + memberName + "' defined");
		}
		return (RelationUpdateItemBuild) fullRUIs[indexR.intValue()];
	}

	public PrimitiveUpdateItem ensurePrimitive(String memberName) {
		PrimitiveUpdateItem pui = findPrimitive(memberName);
		if (pui != null) {
			return pui;
		}
		pui = new PrimitiveUpdateItem();
		pui.setMemberName(memberName);
		addPrimitive(pui);
		return pui;
	}

	public RelationUpdateItemBuild ensureRelation(String memberName) {
		RelationUpdateItemBuild rui = findRelation(memberName);
		if (rui != null) {
			return rui;
		}
		rui = new RelationUpdateItemBuild(memberName);
		addRelation(rui);
		return rui;
	}

	public int getPuiCount() {
		return puiCount;
	}

	public int getRuiCount() {
		return ruiCount;
	}

	@Override
	public void toString(StringBuilder sb) {
		if (isCreate()) {
			sb.append(CreateContainer.class.getSimpleName()).append(": ");
		}
		else if (isUpdate()) {
			sb.append(UpdateContainer.class.getSimpleName()).append(": ");
		}
		else {
			super.toString(sb);
			return;
		}
		StringBuilderUtil.appendPrintable(sb, reference);
	}

	public ICreateOrUpdateContainer build() {
		if (isCreate()) {
			CreateContainer cc = new CreateContainer();
			cc.setReference(getReference());
			cc.setPrimitives(cudResultHelper.compactPUIs(getFullPUIs(), getPuiCount()));
			cc.setRelations(cudResultHelper.compactRUIs(getFullRUIs(), getRuiCount()));
			return cc;
		}
		if (isUpdate()) {
			UpdateContainer uc = new UpdateContainer();
			uc.setReference(getReference());
			uc.setPrimitives(cudResultHelper.compactPUIs(getFullPUIs(), getPuiCount()));
			uc.setRelations(cudResultHelper.compactRUIs(getFullRUIs(), getRuiCount()));
			return uc;
		}
		throw new IllegalStateException("Must never happen");
	}
}
