/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

/**
 * A property that combines other properties in a nested object.
 *
 *
 */
public class GroupProperty extends Property<PropertyMap> implements PropertyGroup<PropertyMap> {

	private static final Logger logger = LoggerFactory.getLogger(GroupProperty.class.getName());

	// indicates whether this group property is
	protected Map<String, PropertyKey> propertyKeys    = new LinkedHashMap<>();
	protected Class<? extends GraphObject> entityClass = null;
	protected Property<Boolean> nullValuesOnlyProperty = null;

	public GroupProperty(String name, Class<? extends GraphObject> entityClass, PropertyKey... properties) {

		super(name);

		for (PropertyKey key : properties) {
			propertyKeys.put(key.jsonName(), key);
			key.dbName(name.concat(".").concat(key.dbName()));
		}

		this.nullValuesOnlyProperty = new BooleanProperty(name.concat(".").concat("nullValuesOnly"));
		this.entityClass            = entityClass;

		// register in entity context
		// FIXME: StructrApp.getConfiguration().registerProperty(entityClass, nullValuesOnlyProperty);
		StructrApp.getConfiguration().registerPropertyGroup(entityClass, this, this);
	}

	@Override
	public GroupProperty indexed() {

		for (PropertyKey key : propertyKeys.values()) {
			key.indexed();
		}

		return (GroupProperty)super.indexed();
	}


	@Override
	public GroupProperty passivelyIndexed() {

		for (PropertyKey key : propertyKeys.values()) {
			key.passivelyIndexed();
		}

		return (GroupProperty)super.passivelyIndexed();
	}

	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return null;
	}

	@Override
	public PropertyConverter<Map<String, Object>, PropertyMap> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, PropertyMap searchValues, boolean exactMatch, Query query) {

		SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (PropertyKey key : propertyKeys.values()) {

			Object value = searchValues.get(new GenericProperty(key.jsonName()));
			if (value != null) {

				group.add(new PropertySearchAttribute(key, value.toString(), Occurrence.REQUIRED, exactMatch));
			}
		}

		return group;
	}

	/**
	 * Returns the nested group property for the given name. The PropertyKey returned by
	 * this method can be used to get and/or set the property value in a PropertyMap that
	 * is obtained or stored in the group property.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return property
	 */
	public <T> PropertyKey<T> getNestedProperty(String name, Class<T> type) {

		if (!propertyKeys.containsKey(name)) {
			throw new IllegalArgumentException("GroupProperty " + dbName + " does not contain grouped property " + name + "!");
		}

		return propertyKeys.get(name);
	}

	/**
	 * Returns a wrapped group property that can be used to access a nested group
	 * property directly, i.e. without having to fetch the group first.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return property
	 */
	public <T> PropertyKey<T> getDirectAccessGroupProperty(String name, Class<T> type) {

		if (!propertyKeys.containsKey(name)) {
			throw new IllegalArgumentException("GroupProperty " + dbName + " does not contain grouped property " + name + "!");
		}

		return new GenericProperty(propertyKeys.get(name).dbName());
	}

	private class InputConverter extends PropertyConverter<Map<String, Object>, PropertyMap> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Map<String, Object> revert(PropertyMap source) throws FrameworkException {
			return PropertyMap.javaTypeToInputType(securityContext, entityClass, source);
		}

		@Override
		public PropertyMap convert(Map<String, Object> source) throws FrameworkException {
			return PropertyMap.inputTypeToJavaType(securityContext, entityClass, source);
		}
	}

	// ----- interface PropertyGroup -----
	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		PropertyMap groupedProperties = new PropertyMap();
		Boolean nullValuesOnly        = source.getProperty(nullValuesOnlyProperty);

		// return immediately, as there are no properties in this group
		if (nullValuesOnly != null && nullValuesOnly.booleanValue()) {
			return null;
		}

		for (PropertyKey key : propertyKeys.values()) {

			Object value = source.getProperty(key);

			groupedProperties.put(key, value);
			if (value != null) {

				nullValuesOnly = false;
			}

		}

		return groupedProperties;
	}

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if (source.containsKey(nullValuesOnlyProperty)) {
			throw new FrameworkException(422, "Property " + jsonName + " is read-only", new ReadOnlyPropertyToken(destination.getClass().getSimpleName(), nullValuesOnlyProperty));
		}

		if (source.isEmpty()) {

			destination.setProperty(nullValuesOnlyProperty, true);

			return;

		}

		// indicate that this group actually contains values
		destination.setProperty(nullValuesOnlyProperty, false);

		// set properties
		for (PropertyKey key : propertyKeys.values()) {

			Object value = source.get(new GenericProperty(key.jsonName()));

			PropertyConverter converter = key.inputConverter(securityContext);
			if (converter != null) {

				try {
					Object convertedValue = converter.convert(value);
					destination.setProperty(key, convertedValue);

				} catch(FrameworkException fex) {

					logger.warn("Unable to convert grouped property {} on type {}: {}", new Object[] {
						key.dbName(),
						source.getClass().getSimpleName(),
						fex.getMessage()

					});
				}


			} else {

				destination.setProperty(key, value);
			}

		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public PropertyMap getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public PropertyMap getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {
		return getGroupedProperties(securityContext, obj);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, PropertyMap value) throws FrameworkException {

		setGroupedProperties(securityContext, value, obj);
		return null;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Class valueType() {
		return PropertyMap.class;
	}

	@Override
	public void setDeclaringClass(Class declaringClass) {

		for (PropertyKey key : propertyKeys.values()) {

			key.setDeclaringClass(declaringClass);
		}
	}

	@Override
	public void extractSearchableAttribute(SecurityContext securityContext, HttpServletRequest request, final boolean exactMatch, final Query query) throws FrameworkException {

		 for (PropertyKey propertyKey : propertyKeys.values()) {

			if (propertyKey instanceof Property) {

				Property key = (Property)propertyKey;

				// use dbName for searching in group properties..
				String searchValue = request.getParameter(key.dbName());
				if (searchValue != null) {

					key.determineSearchType(securityContext, searchValue, exactMatch, query);
				}
			}
		}
	}
}
