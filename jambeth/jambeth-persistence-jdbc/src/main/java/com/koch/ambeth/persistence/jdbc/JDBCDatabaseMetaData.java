package com.koch.ambeth.persistence.jdbc;

/*-
 * #%L
 * jambeth-persistence-jdbc
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.koch.ambeth.ioc.DefaultExtendableContainer;
import com.koch.ambeth.ioc.IBeanRuntime;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.extendable.IExtendableContainer;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.transfer.ObjRef;
import com.koch.ambeth.persistence.DatabaseMetaData;
import com.koch.ambeth.persistence.DirectedExternalLinkMetaData;
import com.koch.ambeth.persistence.DirectedLinkMetaData;
import com.koch.ambeth.persistence.FieldMetaData;
import com.koch.ambeth.persistence.IColumnEntry;
import com.koch.ambeth.persistence.IConnectionDialect;
import com.koch.ambeth.persistence.IPersistenceHelper;
import com.koch.ambeth.persistence.PermissionGroup;
import com.koch.ambeth.persistence.TableMetaData;
import com.koch.ambeth.persistence.api.IFieldMetaData;
import com.koch.ambeth.persistence.api.ILinkMetaData;
import com.koch.ambeth.persistence.api.IPrimaryKeyProvider;
import com.koch.ambeth.persistence.api.ITableMetaData;
import com.koch.ambeth.persistence.api.sql.ISqlBuilder;
import com.koch.ambeth.persistence.config.PersistenceConfigurationConstants;
import com.koch.ambeth.persistence.database.IDatabaseMappedListener;
import com.koch.ambeth.persistence.database.IDatabaseMappedListenerExtendable;
import com.koch.ambeth.persistence.database.IDatabaseMapper;
import com.koch.ambeth.persistence.jdbc.config.PersistenceJdbcConfigurationConstants;
import com.koch.ambeth.persistence.log.PersistenceWarnUtil;
import com.koch.ambeth.persistence.orm.IOrmPatternMatcher;
import com.koch.ambeth.persistence.orm.XmlDatabaseMapper;
import com.koch.ambeth.persistence.sql.ISqlConnection;
import com.koch.ambeth.persistence.sql.ISqlKeywordRegistry;
import com.koch.ambeth.persistence.sql.SqlLinkMetaData;
import com.koch.ambeth.persistence.sql.SqlTableMetaData;
import com.koch.ambeth.util.IConversionHelper;
import com.koch.ambeth.util.NamedItemComparator;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.collections.HashMap;
import com.koch.ambeth.util.collections.HashSet;
import com.koch.ambeth.util.collections.ILinkedMap;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.collections.IMap;
import com.koch.ambeth.util.collections.LinkedHashMap;
import com.koch.ambeth.util.collections.LinkedHashSet;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.objectcollector.IThreadLocalObjectCollector;

public class JDBCDatabaseMetaData extends DatabaseMetaData
		implements IDatabaseMappedListenerExtendable {
	private static final NamedItemComparator namedItemComparator = new NamedItemComparator();

	protected static final Pattern recycleBin = Pattern.compile("BIN\\$.{22}==\\$0",
			Pattern.CASE_INSENSITIVE);

	@LogInstance
	private ILogger log;

	@Autowired
	protected ISqlConnection sqlConnection;

	@Autowired
	protected ISqlBuilder sqlBuilder;

	@Autowired
	protected ISqlKeywordRegistry sqlKeywordRegistry;

	@Autowired
	protected IOrmPatternMatcher ormPatternMatcher;

	@Autowired
	protected IPersistenceHelper persistenceHelper;

	@Autowired
	protected IConversionHelper conversionHelper;

	@Autowired
	protected IConnectionDialect connectionDialect;

	@Autowired
	protected IPrimaryKeyProvider primaryKeyProvider;

	protected final IExtendableContainer<IDatabaseMappedListener> listeners = new DefaultExtendableContainer<>(
			IDatabaseMappedListener.class, "databaseMappedListener");

	@Property(name = PersistenceJdbcConfigurationConstants.DatabaseSchemaName)
	protected String schemaName;

	protected String[] schemaNames;

	protected long lastTestTime = System.currentTimeMillis(), trustTime = 10000;

	protected boolean singleSchema;

	protected String defaultVersionFieldName;

	protected String defaultCreatedByFieldName;

	protected String defaultCreatedOnFieldName;

	protected String defaultUpdatedByFieldName;

	protected String defaultUpdatedOnFieldName;

	protected final HashSet<String> viewNames = new HashSet<>();

	protected final HashSet<String> ignoredTables = new HashSet<>();

	protected final HashSet<String> linkArchiveTables = new HashSet<>();

	@Override
	public void afterPropertiesSet() throws Throwable {
		super.afterPropertiesSet();

		schemaNames = connectionDialect.toDefaultCase(schemaName).split("[:;]");
		singleSchema = schemaNames.length == 1;
	}

	protected SqlLinkMetaData buildAndMapLink(String linkName, ITableMetaData fromTable,
			IFieldMetaData fromField, ITableMetaData toTable, IFieldMetaData toField, boolean nullable) {
		SqlLinkMetaData link = new SqlLinkMetaData();

		link.setName(linkName);
		link.setFromTable(fromTable);
		link.setFromField(fromField);
		link.setToTable(toTable);
		link.setToField(toField);
		link.setNullable(nullable);

		DirectedLinkMetaData directedLink = new DirectedLinkMetaData();
		directedLink.setLink(link);
		directedLink.setFromTable(fromTable);
		directedLink.setFromField(fromField);
		directedLink.setToTable(toTable);
		directedLink.setToField(toField);
		directedLink.setObjectCollector(objectCollector);
		directedLink.setReverse(false);
		directedLink.afterPropertiesSet();

		DirectedLinkMetaData revDirectedLink = new DirectedLinkMetaData();
		revDirectedLink.setLink(link);
		revDirectedLink.setFromTable(toTable);
		revDirectedLink.setFromField(toField);
		revDirectedLink.setToTable(fromTable);
		revDirectedLink.setToField(fromField);
		revDirectedLink.setObjectCollector(objectCollector);
		revDirectedLink.setReverse(true);
		revDirectedLink.afterPropertiesSet();

		link.setDirectedLink(directedLink);
		link.setReverseDirectedLink(revDirectedLink);

		link = (SqlLinkMetaData) mapLink(link);

		return link;
	}

	protected SqlTableMetaData createOneTable(Connection connection, Set<String> fkFields,
			List<FieldMetaData> fields, List<String> pkFieldNames, String fqTableName)
			throws SQLException {
		boolean isPermissionGroupTable = ormPatternMatcher.matchesPermissionGroupPattern(fqTableName);
		if (log.isDebugEnabled()) {
			if (isPermissionGroupTable) {
				log.debug("Recognizing table " + fqTableName + " as permission group table");
			}
			else {
				log.debug("Recognizing table " + fqTableName + " as entity table waiting");
			}
		}
		SqlTableMetaData table = serviceContext.registerAnonymousBean(SqlTableMetaData.class)//
				.propertyValue(TableMetaData.P_INITIAL_VERSION, Integer.valueOf(1))//
				.propertyValue(TableMetaData.P_NAME, fqTableName)//
				.propertyValue(TableMetaData.P_FULL_QUALIFIED_ESCAPED_NAME,
						connectionDialect.escapeName(fqTableName))//
				.propertyValue(TableMetaData.P_VIEW_BASED, viewNames.contains(fqTableName)).finish();

		if (pkFieldNames == null) {
			pkFieldNames = new ArrayList<>(0); // Dummy empty list
		}
		FieldMetaData[] pkFields = new FieldMetaData[pkFieldNames.size()];
		handleTechnicalFields(table, pkFieldNames, fields, pkFields);

		if (pkFields.length > 1 && !isPermissionGroupTable && log.isWarnEnabled()) {
			log.warn("TableMetaData '" + table.getName()
					+ "' has a composite primary key which is currently not supported.");
		}
		if (pkFields.length > 0) {
			table.setIdFields(pkFields);
			pkFields[0].setIdIndex(ObjRef.PRIMARY_KEY_INDEX);
		}

		ILinkedMap<String, String[]> uniqueNameToFieldsMap = getUniqueConstraints(connection, table);
		Collections.sort(fields, namedItemComparator);

		for (int a = 0, size = fields.size(); a < size; a++) {
			FieldMetaData field = fields.get(a);
			field.setTable(table);
			table.mapField(field);
		}
		List<IFieldMetaData> alternateIdFields = new ArrayList<>();
		for (Entry<String, String[]> entry : uniqueNameToFieldsMap) {
			String[] columnNames = entry.getValue();
			if (columnNames.length != 1) {
				continue;
			}
			String fkFieldsKey = tableAndFieldToFKKey(fqTableName, columnNames[0]);
			if (fkFields.contains(fkFieldsKey)) {
				continue;
			}
			// Single column unique constraints can be handled as alternate keys...
			String columnName = columnNames[0];
			IFieldMetaData uniqueConstraintField = table.getFieldByName(columnName);
			((FieldMetaData) uniqueConstraintField).setAlternateId();
			((FieldMetaData) uniqueConstraintField).setIdIndex((byte) alternateIdFields.size());
			alternateIdFields.add(uniqueConstraintField);
		}
		table.setAlternateIdFields(
				alternateIdFields.toArray(new IFieldMetaData[alternateIdFields.size()]));

		getTables().add(table);

		putTableByName(fqTableName, table);
		return table;
	}

	protected int fieldsContainPermissionGroup(List<FieldMetaData> fields) {
		int permissionGroupCount = 0;
		for (FieldMetaData field : fields) {
			if (PermissionGroup.permGroupIdNameOfData.equals(field.getName())) {
				permissionGroupCount++;
			}
			Matcher matcher = PermissionGroup.permGroupFieldForLink.matcher(field.getName());
			if (matcher.matches()) {
				permissionGroupCount++;
			}
		}
		return permissionGroupCount;
	}

	protected void findAndAssignFulltextFields(Connection connection) throws SQLException {
		for (String schemaName : schemaNames) {
			ILinkedMap<String, IList<String>> fulltextIndizes = connectionDialect
					.getFulltextIndexes(connection, schemaName);

			for (Entry<String, IList<String>> entry : fulltextIndizes) {
				String tableName;
				tableName = entry.getKey();
				if (!singleSchema) {
					tableName = schemaName + "." + tableName;
				}
				ITableMetaData table = nameToTableDict.get(tableName);
				IList<String> fieldNames = entry.getValue();
				if (table != null && table instanceof TableMetaData) {
					((TableMetaData) table).setFulltextFieldsByNames(fieldNames);
				}
			}
		}
	}

	protected ILinkedMap<String, IColumnEntry> getCachedFieldValues(Connection connection,
			String tableName) throws SQLException {
		ILinkedMap<String, IColumnEntry> cachedFieldValues = new LinkedHashMap<>();

		IList<IColumnEntry> allFieldsOfTable = connectionDialect.getAllFieldsOfTable(connection,
				tableName);

		for (IColumnEntry entry : allFieldsOfTable) {
			cachedFieldValues.put(entry.getFieldName(), entry);
		}
		return cachedFieldValues;
	}

	protected List<FieldMetaData> getFields(Connection connection, String tableName)
			throws SQLException {
		ILinkedMap<String, IColumnEntry> cachedFieldValues = getCachedFieldValues(connection,
				tableName);
		ArrayList<FieldMetaData> fields = new ArrayList<>(cachedFieldValues.size());
		for (Entry<String, IColumnEntry> entry : cachedFieldValues) {
			IColumnEntry columnEntry = entry.getValue();
			Class<?> fieldSubType = connectionDialect
					.getComponentTypeByFieldTypeName(columnEntry.getTypeName());

			FieldMetaData field = new FieldMetaData();
			field.setExpectsMapping(columnEntry.expectsMapping());
			field.setName(columnEntry.getFieldName());
			field.setOriginalTypeName(columnEntry.getTypeName());
			field.setFieldType(columnEntry.getJavaType());
			field.setFieldSubType(fieldSubType);

			fields.add(field);
		}
		return fields;
	}

	protected int getPrimaryKeyCount(Connection connection, String tableName) throws SQLException {
		String[] names = sqlBuilder.getSchemaAndTableName(tableName);
		ResultSet allPrimaryKeysRS = connection.getMetaData().getPrimaryKeys(null, names[0], names[1]);
		int count = 0;
		try {
			while (allPrimaryKeysRS.next()) {
				count++;
			}
		}
		finally {
			JdbcUtil.close(allPrimaryKeysRS);
		}
		return count;
	}

	@Override
	public String[] getSchemaNames() {
		return schemaNames;
	}

	protected ILinkedMap<String, String[]> getUniqueConstraints(Connection connection,
			ITableMetaData table) throws SQLException {
		String[] names = sqlBuilder.getSchemaAndTableName(table.getName());
		ResultSet allUniqueKeysRS = connectionDialect.getIndexInfo(connection, names[0], names[1],
				true);
		try {
			LinkedHashMap<String, String[]> uniqueNameToFieldsMap = new LinkedHashMap<>();
			IFieldMetaData idField = table.getIdField();
			while (allUniqueKeysRS.next()) {
				String indexName = allUniqueKeysRS.getString("INDEX_NAME");
				if (indexName == null) {
					continue;
				}
				// String tableName = allUniqueKeysRS.getString("TABLE_NAME");
				// boolean nonUnique = allUniqueKeysRS.getBoolean("NON_UNIQUE");
				// String indexQualifier = allUniqueKeysRS.getString("INDEX_QUALIFIER");
				// short type = allUniqueKeysRS.getShort("TYPE");
				short ordinalPosition = allUniqueKeysRS.getShort("ORDINAL_POSITION");
				String columnName = allUniqueKeysRS.getString("COLUMN_NAME");
				if (idField != null && idField.getName().equals(columnName)) {
					continue;
				}
				// String ascOrDesc = allUniqueKeysRS.getString("ASC_OR_DESC");
				// int cardinality = allUniqueKeysRS.getInt("CARDINALITY");

				String[] columnNames = uniqueNameToFieldsMap.get(indexName);
				if (columnNames == null) {
					columnNames = new String[ordinalPosition];
					uniqueNameToFieldsMap.put(indexName, columnNames);
				}
				if (columnNames.length < ordinalPosition) {
					String[] newColumnNames = new String[ordinalPosition];
					System.arraycopy(columnNames, 0, newColumnNames, 0, columnNames.length);
					columnNames = newColumnNames;
					uniqueNameToFieldsMap.put(indexName, columnNames);
				}
				columnNames[ordinalPosition - 1] = columnName;
			}
			return uniqueNameToFieldsMap;
		}
		finally {
			Statement stm = allUniqueKeysRS.getStatement();
			if (stm != null) {
				stm.close();
			}
			JdbcUtil.close(allUniqueKeysRS);
		}
	}

	protected void grepDataTables(Connection connection, Set<String> fqTableNames,
			Set<String> fqDataTableNames, Map<String, List<FieldMetaData>> tableNameToFields,
			Map<String, List<String[]>> linkNameToEntryMap) throws SQLException {
		for (String fqTableName : fqTableNames) {
			List<FieldMetaData> fields = tableNameToFields.get(fqTableName);
			if (isLinkTable(fqTableName, fields, linkNameToEntryMap)
					|| isLinkTableToExtern(connection, fqTableName, fields, linkNameToEntryMap)
					|| isLinkArchiveTable(connection, fqTableName, fields)) {
				// This is a pure link table
				continue;
			}
			fqDataTableNames.add(fqTableName);
		}
	}

	protected void handleLinkTable(Connection connection, String linkName, List<String[]> values)
			throws SQLException {
		// Pure link table
		String pkTableName = values.get(0)[0];
		String pkFieldName = values.get(0)[1];
		String linkColumnName = values.get(0)[3];
		String constraintName = values.get(0)[4];
		String pkTableName2 = values.get(1)[0];
		String pkFieldName2 = values.get(1)[1];
		String linkColumnName2 = values.get(1)[3];
		String constraintName2 = values.get(1)[4];

		ITableMetaData fromTable = null, toTable = null;
		IFieldMetaData fromField = null, toField = null;
		String fromConstraint = null, toConstraint = null;

		ITableMetaData entityTable1 = nameToTableDict.get(pkTableName);
		ITableMetaData entityTable2 = nameToTableDict.get(pkTableName2);
		IFieldMetaData entityIdField1 = entityTable1.getFieldByName(pkFieldName);
		IFieldMetaData entityIdField2 = entityTable2.getFieldByName(pkFieldName2);

		ILinkedMap<String, IColumnEntry> cachedFieldValues = getCachedFieldValues(connection, linkName);
		for (Entry<String, IColumnEntry> entry2 : cachedFieldValues) {
			IColumnEntry entry = entry2.getValue();
			String fieldName = entry.getFieldName();
			int columnIndex = entry.getColumnIndex();

			IBeanRuntime<FieldMetaData> field = serviceContext.registerBean(FieldMetaData.class)//
					.propertyValue(FieldMetaData.P_NAME, fieldName)//
					.propertyValue(FieldMetaData.P_FIELD_TYPE, entry.getJavaType())
					.propertyValue(FieldMetaData.P_ORIGINAL_TYPE_NAME, entry.getTypeName());

			if (fieldName.equals(linkColumnName)) {
				if (columnIndex == 1) // "from" column
				{
					fromTable = entityTable1;
					field.propertyValue(FieldMetaData.P_TABLE, fromTable);
					field.propertyValue(FieldMetaData.P_ID_INDEX, entityIdField1.getIdIndex());
					fromField = field.finish();
					fromConstraint = constraintName;
				}
				else if (columnIndex == 2)// "to" column
				{
					toTable = entityTable1;
					field.propertyValue(FieldMetaData.P_TABLE, toTable);
					field.propertyValue(FieldMetaData.P_ID_INDEX, entityIdField1.getIdIndex());
					toField = field.finish();
					toConstraint = constraintName;
				}
			}
			else if (fieldName.equals(linkColumnName2)) {
				if (columnIndex == 1) // "from" column
				{
					fromTable = entityTable2;
					field.propertyValue(FieldMetaData.P_TABLE, fromTable);
					field.propertyValue(FieldMetaData.P_ID_INDEX, entityIdField2.getIdIndex());
					fromField = field.finish();
					fromConstraint = constraintName2;
				}
				else if (columnIndex == 2)// "to" column
				{
					toTable = entityTable2;
					field.propertyValue(FieldMetaData.P_TABLE, toTable);
					field.propertyValue(FieldMetaData.P_ID_INDEX, entityIdField2.getIdIndex());
					toField = field.finish();
					toConstraint = constraintName2;
				}
			}
		}

		SqlLinkMetaData link = buildAndMapLink(linkName, fromTable, fromField, toTable, toField, true);
		link.setFullqualifiedEscapedTableName(connectionDialect.escapeName(linkName));
		((DirectedLinkMetaData) link.getDirectedLink()).setConstraintName(fromConstraint);
		((DirectedLinkMetaData) link.getDirectedLink()).setStandaloneLink(true);
		((DirectedLinkMetaData) link.getReverseDirectedLink()).setStandaloneLink(true);
		((DirectedLinkMetaData) link.getReverseDirectedLink()).setConstraintName(toConstraint);

		link = serviceContext.registerWithLifecycle(link).finish();

		putLinkByDefiningName(link.getName(), link);
		addLinkByTables(link);

		if (log.isDebugEnabled()) {
			PersistenceWarnUtil.logDebugOnce(log, loggerHistory, connection,
					"Recognizing table '" + link.getName() + "' as link between table '" + fromTable.getName()
							+ "' and '" + toTable.getName() + "'");
		}
	}

	protected void handleLinkTableToExtern(Connection connection, String linkName,
			List<String[]> values) throws SQLException {
		// Pure link table to external entity
		String[] onlyValue = values.get(0);
		String entityTableName = onlyValue[0];
		String entityIdFieldName = onlyValue[1];
		// String linkTableName = onlyValue[2];
		String linkColumnName = onlyValue[3];

		ITableMetaData fromTable = null;
		IFieldMetaData fromField = null, toField = null;
		ITableMetaData entityTable = nameToTableDict.get(entityTableName);
		IFieldMetaData entityIdField = entityTable.getFieldByName(entityIdFieldName);

		ILinkedMap<String, IColumnEntry> cachedFieldValues = getCachedFieldValues(connection, linkName);
		for (Entry<String, IColumnEntry> entry2 : cachedFieldValues) {
			IColumnEntry entry = entry2.getValue();
			String fieldName = entry.getFieldName();

			IBeanRuntime<FieldMetaData> field = serviceContext.registerBean(FieldMetaData.class)
					.propertyValue(FieldMetaData.P_NAME, fieldName)
					.propertyValue(FieldMetaData.P_FIELD_TYPE, entry.getJavaType())
					.propertyValue(FieldMetaData.P_ORIGINAL_TYPE_NAME, entry.getTypeName());

			if (fieldName.equals(linkColumnName)) {
				fromTable = entityTable;
				field.propertyValue(FieldMetaData.P_TABLE, fromTable);
				field.propertyValue(FieldMetaData.P_ID_INDEX, entityIdField.getIdIndex());
				fromField = field.finish();
			}
			else {
				field.propertyValue(FieldMetaData.P_TABLE, new TableMetaData());
				toField = field.finish();
			}
		}

		SqlLinkMetaData link = new SqlLinkMetaData();
		link.setName(linkName);
		link.setFullqualifiedEscapedTableName(connectionDialect.escapeName(linkName));
		link.setFromTable(fromTable);
		link.setFromField(fromField);
		link.setToField(toField);
		link.setNullable(true);

		DirectedExternalLinkMetaData directedLink = new DirectedExternalLinkMetaData();
		directedLink.setLink(link);
		directedLink.setFromTable(fromTable);
		directedLink.setFromField(fromField);
		directedLink.setToField(toField);
		directedLink.setStandaloneLink(true);
		directedLink.setReverse(false);
		directedLink.setObjectCollector(objectCollector);
		// afterPropertiesSet() and additional injections in XmlDatabaseMapper (later)

		DirectedExternalLinkMetaData revDirectedLink = new DirectedExternalLinkMetaData();
		revDirectedLink.setLink(link);
		revDirectedLink.setFromField(toField);
		revDirectedLink.setToTable(fromTable);
		revDirectedLink.setToField(fromField);
		revDirectedLink.setStandaloneLink(true);
		revDirectedLink.setReverse(true);
		revDirectedLink.setObjectCollector(objectCollector);
		// afterPropertiesSet() and additional injections in XmlDatabaseMapper (later)

		link.setDirectedLink(directedLink);
		link.setReverseDirectedLink(revDirectedLink);

		putLinkByName(link.getName(), link);
		putLinkByDefiningName(link.getName(), link);
		addLinkByTables(link);

		if (log.isDebugEnabled()) {
			log.debug("Recognizing table '" + linkName + "' as link between table '" + values.get(0)[0]
					+ "' and an external entity");
		}
	}

	protected void handleLinkWithinDataTable(Connection connection, String linkName,
			List<String[]> values, List<FieldMetaData> fields) throws SQLException {
		// Entity table with links
		for (int i = values.size(); i-- > 0;) {
			String entityTableName = values.get(i)[0];
			String entityColumnName = values.get(i)[1];
			String linkColumnName = values.get(i)[3];
			String constraintName = values.get(i)[4];

			ITableMetaData fromTable;
			IFieldMetaData fromField;
			ITableMetaData toTable;
			IFieldMetaData toField;

			fromTable = nameToTableDict.get(linkName);
			fromField = fromTable.getFieldByName(linkColumnName);
			toTable = nameToTableDict.get(entityTableName);
			toField = toTable.getFieldByName(entityColumnName);

			if (fromField == null) {
				fromField = fromTable.getIdField();
			}
			if (toField == null) {
				toField = toTable.getIdField();
			}
			if (fromField.getIdIndex() == ObjRef.UNDEFINED_KEY_INDEX) {
				((FieldMetaData) fromField).setIdIndex(toField.getIdIndex());
			}
			if (!fromField.isAlternateId()) {
				fromField.getTable().getPrimitiveFields().remove(fromField);
			}
			if (!toField.isAlternateId()) {
				toField.getTable().getPrimitiveFields().remove(toField);
			}
			boolean nullable = isFieldNullable(connection, fromField);

			SqlLinkMetaData link = buildAndMapLink(createForeignKeyLinkName(fromTable.getName(),
					fromField.getName(), toTable.getName(), toField.getName()), fromTable, fromField, toTable,
					toField, nullable);
			link.setConstraintName(constraintName);
			link.setTableName(linkName);
			String[] schemaAndName = XmlDatabaseMapper.splitSchemaAndName(linkName);
			link.setFullqualifiedEscapedTableName(
					connectionDialect.escapeSchemaAndSymbolName(schemaAndName[0], schemaAndName[1]));

			boolean fromIsStandalone = fromField.isAlternateId()
					|| (fromTable.getIdField() != null && fromTable.getIdField().equals(fromField));
			boolean toIsStandalone = toField.isAlternateId() || toTable.getIdField().equals(toField);
			((DirectedLinkMetaData) link.getDirectedLink()).setStandaloneLink(fromIsStandalone);
			((DirectedLinkMetaData) link.getReverseDirectedLink()).setStandaloneLink(toIsStandalone);

			link = serviceContext.registerWithLifecycle(link).finish();

			putLinkByDefiningName(constraintName, link);
			addLinkByTables(link);

			if (log.isDebugEnabled()) {
				log.debug("Recognizing table '" + fromTable.getName() + "' as data table with link ("
						+ link.getName() + ") to table '" + toTable.getName() + "'");
			}
		}
	}

	protected void handleTechnicalFields(final SqlTableMetaData table,
			final List<String> pkFieldNames, final List<FieldMetaData> fields,
			final FieldMetaData[] pkFields) {
		for (int a = fields.size(); a-- > 0;) {
			FieldMetaData field = fields.get(a);
			field.setTable(table);
			String fieldName = field.getName();
			if (fieldName.equalsIgnoreCase(defaultVersionFieldName)) {
				table.setVersionField(field);
				fields.remove(a);
				continue;
			}
			else if (fieldName.equalsIgnoreCase(defaultCreatedByFieldName)) {
				table.setCreatedByField(field);
				// fields.remove(a);
				continue;
			}
			else if (fieldName.equalsIgnoreCase(defaultCreatedOnFieldName)) {
				table.setCreatedOnField(field);
				// fields.remove(a);
				continue;
			}
			else if (fieldName.equalsIgnoreCase(defaultUpdatedByFieldName)) {
				table.setUpdatedByField(field);
				// fields.remove(a);
				continue;
			}
			else if (fieldName.equalsIgnoreCase(defaultUpdatedOnFieldName)) {
				table.setUpdatedOnField(field);
				// fields.remove(a);
				continue;
			}
			else if ("rowid".equalsIgnoreCase(fieldName)) {
				continue;
			}
			for (int b = pkFieldNames.size(); b-- > 0;) {
				if (pkFieldNames.get(b).equalsIgnoreCase(fieldName)) {
					pkFields[b] = field;
					fields.remove(a);
					break;
				}
			}
		}
	}

	public void init(Connection connection) {
		try {
			setMaxNameLength(connection.getMetaData().getMaxProcedureNameLength());

			LinkedHashMap<String, List<String[]>> linkNameToEntryMap = new LinkedHashMap<>();
			Set<String> fkFields = new HashSet<>();
			Set<String> fqTableNames = new LinkedHashSet<>();
			Set<String> fqDataTableNames = new LinkedHashSet<>();
			final Map<String, List<FieldMetaData>> tableNameToFields = new HashMap<>();
			Map<String, List<String>> tableNameToPkFieldsMap = new HashMap<>();

			registerSqlKeywords(connection);

			// Load required database metadata
			loadLinkInfos(connection, linkNameToEntryMap, fkFields);
			loadTableNames(connection, fqTableNames);
			loadTableFields(connection, fqTableNames, tableNameToFields);
			grepDataTables(connection, fqTableNames, fqDataTableNames, tableNameToFields,
					linkNameToEntryMap);
			mapPrimaryKeys(connection, fqDataTableNames, tableNameToPkFieldsMap);

			if (fqDataTableNames.isEmpty() && log.isWarnEnabled()) {
				log.warn("Schema '" + schemaName + "' contains no data tables");
			}

			for (String fqTableName : fqDataTableNames) {
				createOneTable(connection, fkFields, tableNameToFields.get(fqTableName),
						tableNameToPkFieldsMap.get(fqTableName), fqTableName);
			}
			Collections.sort(getTables(), new Comparator<ITableMetaData>() {
				@Override
				public int compare(ITableMetaData o1, ITableMetaData o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			findAndAssignFulltextFields(connection);

			for (Entry<String, List<String[]>> entry : linkNameToEntryMap) {
				String linkName = entry.getKey();
				List<String[]> values = entry.getValue();
				List<FieldMetaData> fields = tableNameToFields.get(linkName);
				ITableMetaData table = nameToTableDict.get(linkName);

				int hasPermissionGroupField = fieldsContainPermissionGroup(fields);

				if (table != null) {
					handleLinkWithinDataTable(connection, linkName, values, fields);
				}
				else if (((fields.size() - hasPermissionGroupField) == connectionDialect
						.getColumnCountForLinkTable()) && values.size() == 2) {
					handleLinkTable(connection, linkName, values);
				}
				else if (((fields.size() - hasPermissionGroupField) == connectionDialect
						.getColumnCountForLinkTable()) && values.size() == 1) {
					handleLinkTableToExtern(connection, linkName, values);
				}
				else {
					throw new IllegalStateException("Type of link can not be determined: '" + linkName + "'");
				}
			}

			List<ITableMetaData> tables = getTables();
			for (int a = tables.size(); a-- > 0;) {
				ITableMetaData table = tables.get(a);
				tables.set(a, table);
			}
			List<ILinkMetaData> links = getLinks();
			for (int a = links.size(); a-- > 0;) {
				ILinkMetaData link = links.get(a);
				link = serviceContext.registerWithLifecycle(link).finish();
				links.set(a, link);
			}
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
		IList<IDatabaseMapper> objects = serviceContext.getObjects(IDatabaseMapper.class);
		for (int a = objects.size(); a-- > 0;) {
			objects.get(a).mapFields(connection, schemaNames, this);
		}
		for (int a = objects.size(); a-- > 0;) {
			objects.get(a).mapLinks(connection, schemaNames, this);
		}
		int maxNameLength = getMaxNameLength();
		for (ITableMetaData table : getTables()) {
			if (table.isPermissionGroup()) {
				continue;
			}
			ITableMetaData permissionGroupTable = getTableByName(
					ormPatternMatcher.buildPermissionGroupFromTableName(table.getName(), maxNameLength));
			if (permissionGroupTable != null) {
				mapPermissionGroupTable(permissionGroupTable, table);
			}
		}
		super.afterStarted();

		IList<IDatabaseMappedListener> databaseMappedListeners = serviceContext
				.getObjects(IDatabaseMappedListener.class);
		for (int a = databaseMappedListeners.size(); a-- > 0;) {
			databaseMappedListeners.get(a).databaseMapped(this);
		}
	}

	@Override
	public boolean isFieldNullable(Connection connection, IFieldMetaData field) throws SQLException {
		boolean nullable = false;

		String tableName = field.getTable().getName();
		ILinkedMap<String, IColumnEntry> cachedFieldValues = getCachedFieldValues(connection,
				tableName);
		IColumnEntry entry = cachedFieldValues.get(field.getName());
		nullable = entry.isNullable();

		return nullable;
	}

	protected boolean isLinkArchiveTable(Connection connection, String tableName,
			List<FieldMetaData> fields) throws SQLException {
		if (fields.size() != 4) {
			return false;
		}
		String[] archiveFieldNames = { "ARCHIVED_ON", "ARCHIVED_BY" };
		boolean archiveFieldNamesFound = false;
		int toFind = archiveFieldNames.length;
		for (int i = fields.size(); i-- > 0;) {
			String fieldName = fields.get(i).getName();
			if (fieldName.equals(archiveFieldNames[0]) || fieldName.equals(archiveFieldNames[1])) {
				toFind--;
			}
			if (toFind == 0) {
				archiveFieldNamesFound = true;
				break;
			}
		}
		// TODO check: Primarykey aus 3 Spalten, eine davon heißt ARCHIVED_ON, die anderen beiden
		// nicht
		// ARCHIVED_BY
		if (archiveFieldNamesFound && getPrimaryKeyCount(connection, tableName) == 3) {
			linkArchiveTables.add(tableName);
		}
		return archiveFieldNamesFound;
	}

	@Override
	public boolean isLinkArchiveTable(String tableName) {
		return linkArchiveTables.contains(tableName);
	}

	protected boolean isLinkTable(String tableName, List<FieldMetaData> fields,
			Map<String, List<String[]>> linkNameToEntryMap) {
		if (!linkNameToEntryMap.containsKey(tableName)
				|| linkNameToEntryMap.get(tableName).size() != 2) {
			return false;
		}
		int hasPermissionGroupField = fieldsContainPermissionGroup(fields);
		return ((fields.size() - hasPermissionGroupField) == connectionDialect
				.getColumnCountForLinkTable());
	}

	protected boolean isLinkTableToExtern(Connection connection, String tableName,
			List<FieldMetaData> fields, Map<String, List<String[]>> linkNameToEntryMap)
			throws SQLException {
		return linkNameToEntryMap.containsKey(tableName) && fields.size() == 3
				&& linkNameToEntryMap.get(tableName).size() == 1
				&& getPrimaryKeyCount(connection, tableName) == 2;
	}

	protected void loadLinkInfos(Connection connection,
			Map<String, List<String[]>> linkNameToEntryMap, Set<String> fkFields) throws SQLException {
		IList<IMap<String, String>> allForeignKeys = connectionDialect.getExportedKeys(connection,
				schemaNames);

		StringBuilder sb = objectCollector.create(StringBuilder.class);
		try {
			for (int i = allForeignKeys.size(); i-- > 0;) {
				IMap<String, String> foreignKey = allForeignKeys.get(i);
				try {
					String schemaName = foreignKey.get("OWNER");
					String constraintName = foreignKey.get("CONSTRAINT_NAME");
					String pkTableName = foreignKey.get("PKTABLE_NAME");
					String pkColumnName = foreignKey.get("PKCOLUMN_NAME");
					String linkTableName = foreignKey.get("FKTABLE_NAME");
					String linkColumnName = foreignKey.get("FKCOLUMN_NAME");

					sb.setLength(0);
					pkTableName = sb.append(schemaName).append(".").append(pkTableName).toString();
					sb.setLength(0);
					linkTableName = sb.append(schemaName).append(".").append(linkTableName).toString();

					List<String[]> tableLinks = linkNameToEntryMap.get(linkTableName);
					if (tableLinks == null) {
						tableLinks = new ArrayList<>();
						linkNameToEntryMap.put(linkTableName, tableLinks);
					}
					tableLinks.add(new String[] { pkTableName, pkColumnName, linkTableName, linkColumnName,
							constraintName });
					fkFields.add(tableAndFieldToFKKey(linkTableName, linkColumnName));
				}
				finally {
					// foreignKey.dispose();
				}
			}
		}
		finally {
			// allForeignKeys.dispose();
			objectCollector.dispose(sb);
		}
	}

	protected void loadTableFields(Connection connection, Set<String> tableNames,
			Map<String, List<FieldMetaData>> tableNameToFields) throws SQLException {
		for (String tableName : tableNames) {
			List<FieldMetaData> fields = getFields(connection, tableName);
			tableNameToFields.put(tableName, fields);
		}
	}

	protected void loadTableNames(Connection connection, Set<String> fqTableNames)
			throws SQLException {
		IThreadLocalObjectCollector objectCollector = this.objectCollector.getCurrent();
		StringBuilder sb = objectCollector.create(StringBuilder.class);
		try {
			List<String> fqTableNamesList = connectionDialect.getAllFullqualifiedTableNames(connection,
					schemaNames);
			List<String> fqViewNamesList = connectionDialect.getAllFullqualifiedViews(connection,
					schemaNames);
			for (String fqTableName : fqTableNamesList) {
				if (ignoredTables.contains(fqTableName)) {
					continue; // this is not a table we are interested in
				}
				String[] schemaAndName = XmlDatabaseMapper.splitSchemaAndName(fqTableName);
				if (ignoredTables.contains(schemaAndName[1])) // look with the soft name
				{
					continue; // this is not a table we are interested in
				}
				fqTableNames.add(fqTableName);
			}
			for (String fqViewName : fqViewNamesList) {
				if (ignoredTables.contains(fqViewName)) {
					continue; // this is not a table we are interested in
				}
				String[] schemaAndName = XmlDatabaseMapper.splitSchemaAndName(fqViewName);
				if (ignoredTables.contains(schemaAndName[1])) // look with the soft name
				{
					continue; // this is not a table we are interested in
				}
				fqTableNames.add(fqViewName);
				viewNames.add(fqViewName);
			}
		}
		finally {
			objectCollector.dispose(sb);
		}
	}

	@Override
	public ILinkMetaData mapLink(ILinkMetaData link) {
		putLinkByName(link.getName(), link);
		links.add(link);

		TableMetaData fromTable = (TableMetaData) link.getFromTable();
		TableMetaData toTable = (TableMetaData) link.getToTable();

		fromTable.mapLink(link.getDirectedLink());
		if (toTable != null) {
			toTable.mapLink(link.getReverseDirectedLink());
		}

		link = serviceContext.registerWithLifecycle(link).finish();

		return link;
	}

	protected void mapPrimaryKeys(Connection connection, Set<String> tableNames,
			Map<String, List<String>> tableNameToPkFieldsMap) throws SQLException {
		Iterator<String> iter = tableNames.iterator();
		while (iter.hasNext()) {
			String tableName = iter.next();
			String[] names = sqlBuilder.getSchemaAndTableName(tableName);
			ResultSet allPrimaryKeysRS = connection.getMetaData().getPrimaryKeys(null, names[0],
					names[1]);
			try {
				while (allPrimaryKeysRS.next()) {
					String columnName = allPrimaryKeysRS.getString("COLUMN_NAME");
					short keySeq = allPrimaryKeysRS.getShort("KEY_SEQ");

					List<String> tableList = tableNameToPkFieldsMap.get(tableName);
					if (tableList == null) {
						tableList = new ArrayList<>();
						tableNameToPkFieldsMap.put(tableName, tableList);
					}
					while (tableList.size() < keySeq) {
						tableList.add(null);
					}
					tableList.set(keySeq - 1, columnName);
				}
			}
			finally {
				JdbcUtil.close(allPrimaryKeysRS);
			}
		}
	}

	protected void putLinkByDefiningName(String name, ILinkMetaData link) {
		putObjectByName(name, link, definingNameToLinkDict);
	}

	protected void putLinkByName(String name, ILinkMetaData link) {
		putObjectByName(name, link, nameToLinkDict);
	}

	protected <T> void putObjectByName(String name, T link, IMap<String, T> nameToObjectMap) {
		String[] schemaAndName = XmlDatabaseMapper.splitSchemaAndName(name);
		String schemaName = schemaAndName[0];
		String softName = schemaAndName[1];
		if (schemaName == null) {
			schemaName = schemaNames[0];
		}
		if (schemaName.equals(schemaNames[0])) {
			nameToObjectMap.put(softName, link);
			nameToObjectMap.put(softName.toUpperCase(), link);
			nameToObjectMap.put(softName.toLowerCase(), link);
		}
		nameToObjectMap.putIfNotExists(schemaName + "." + softName, link);
		nameToObjectMap.putIfNotExists(schemaName + "." + softName.toUpperCase(), link);
		nameToObjectMap.putIfNotExists(schemaName + "." + softName.toLowerCase(), link);
		nameToObjectMap.putIfNotExists((schemaName + "." + softName).toUpperCase(), link);
		nameToObjectMap.putIfNotExists((schemaName + "." + softName).toLowerCase(), link);
		nameToObjectMap.putIfNotExists(schemaName.toUpperCase() + "." + softName, link);
		nameToObjectMap.putIfNotExists(schemaName.toUpperCase() + "." + softName.toUpperCase(), link);
		nameToObjectMap.putIfNotExists(schemaName.toUpperCase() + "." + softName.toLowerCase(), link);
		nameToObjectMap.putIfNotExists(schemaName.toLowerCase() + "." + softName, link);
		nameToObjectMap.putIfNotExists(schemaName.toLowerCase() + "." + softName.toUpperCase(), link);
		nameToObjectMap.putIfNotExists(schemaName.toLowerCase() + "." + softName.toLowerCase(), link);
	}

	protected void putTableByName(String name, ITableMetaData table) {
		putObjectByName(name, table, nameToTableDict);
	}

	@Override
	public void registerDatabaseMappedListener(IDatabaseMappedListener databaseMappedListener) {
		listeners.register(databaseMappedListener);
	}

	@Override
	public ITableMetaData registerNewTable(Connection connection, String fqTableName) {
		try {
			if (!XmlDatabaseMapper.fqToSoftTableNamePattern.matcher(fqTableName).matches()) {
				fqTableName = getSchemaNames()[0] + "." + fqTableName;
			}
			Set<String> fqDataTableNames = new LinkedHashSet<>();
			fqDataTableNames.add(fqTableName);

			LinkedHashMap<String, List<String[]>> linkNameToEntryMap = new LinkedHashMap<>();
			Set<String> fkFields = new HashSet<>();
			final Map<String, List<FieldMetaData>> tableNameToFields = new HashMap<>();
			Map<String, List<String>> tableNameToPkFieldsMap = new HashMap<>();

			// Load required database metadata
			loadLinkInfos(connection, linkNameToEntryMap, fkFields);
			loadTableFields(connection, fqDataTableNames, tableNameToFields);
			mapPrimaryKeys(connection, fqDataTableNames, tableNameToPkFieldsMap);

			ITableMetaData newTable = createOneTable(connection, fkFields,
					tableNameToFields.get(fqTableName), tableNameToPkFieldsMap.get(fqTableName), fqTableName);

			for (Entry<String, List<String[]>> entry : linkNameToEntryMap) {
				String linkName = entry.getKey();
				if (!fqTableName.equals(linkName)) {
					// Only handle links that are from the newly created table
					continue;
				}
				List<String[]> values = entry.getValue();
				List<FieldMetaData> fields = tableNameToFields.get(linkName);
				ITableMetaData table = nameToTableDict.get(linkName);

				int hasPermissionGroupField = fieldsContainPermissionGroup(fields);
				boolean isLinkTable = ((fields.size() - hasPermissionGroupField) == connectionDialect
						.getColumnCountForLinkTable());

				if (table != null) {
					handleLinkWithinDataTable(connection, linkName, values, fields);
				}
				else if (isLinkTable && values.size() == 2) {
					handleLinkTable(connection, linkName, values);
				}
				else if (isLinkTable && values.size() == 1) {
					handleLinkTableToExtern(connection, linkName, values);
				}
				else {
					throw new IllegalStateException("Type of link can not be determined: '" + linkName + "'");
				}
			}
			return newTable;
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	protected void registerSqlKeywords(Connection connection) throws SQLException {
		String[] keywords = connection.getMetaData().getSQLKeywords().split(",");
		for (int a = keywords.length; a-- > 0;) {
			sqlKeywordRegistry.registerSqlKeyword(keywords[a].trim());
		}
	}

	public void setDefaultCreatedByFieldName(String defaultCreatedByFieldName) {
		this.defaultCreatedByFieldName = defaultCreatedByFieldName;
	}

	public void setDefaultCreatedOnFieldName(String defaultCreatedOnFieldName) {
		this.defaultCreatedOnFieldName = defaultCreatedOnFieldName;
	}

	public void setDefaultUpdatedByFieldName(String defaultUpdatedByFieldName) {
		this.defaultUpdatedByFieldName = defaultUpdatedByFieldName;
	}

	public void setDefaultUpdatedOnFieldName(String defaultUpdatedOnFieldName) {
		this.defaultUpdatedOnFieldName = defaultUpdatedOnFieldName;
	}

	public void setDefaultVersionFieldName(String defaultVersionFieldName) {
		this.defaultVersionFieldName = defaultVersionFieldName;
	}

	@Property(name = PersistenceConfigurationConstants.DatabaseTableIgnore, mandatory = false)
	public void setIgnoredTables(String ignoredTables) {
		String[] splitTableNames = ignoredTables.split("[:;]");
		this.ignoredTables.clear();
		this.ignoredTables.addAll(Arrays.asList(splitTableNames));
	}

	protected String tableAndFieldToFKKey(String tableName, String fieldName) {
		return tableName + "'''" + fieldName; // ''' will never ever come up in a sql name
	}

	@Override
	public void unregisterDatabaseMappedListener(IDatabaseMappedListener databaseMappedListener) {
		listeners.unregister(databaseMappedListener);
	}
}
