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
package org.structr.core.function;

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetOrNullFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_OR_NULL    = "Usage: ${get_or_null(entity, propertyKey)}. Example: ${get_or_null(this, \"children\")}";
	public static final String ERROR_MESSAGE_GET_OR_NULL_JS = "Usage: ${{Structr.getOrNull(entity, propertyKey)}}. Example: ${{Structr.getOrNull(this, \"children\")}}";

	@Override
	public String getName() {
		return "get_or_null()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
		
		try {
			if (!arrayHasLengthAndAllElementsNotNull(sources, 2)) {
				
				return null;
			}

			GraphObject dataObject = null;

			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			}

			if (sources[0] instanceof List) {

				final List list = (List)sources[0];
				if (list.size() == 1 && list.get(0) instanceof GraphObject) {

					dataObject = (GraphObject)list.get(0);
				}
			}

			if (dataObject != null) {

				final String keyName = sources[1].toString();
				final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(dataObject.getClass(), keyName);

				if (key != null) {

					final PropertyConverter inputConverter = key.inputConverter(securityContext);
					Object value = dataObject.getProperty(key);

					if (inputConverter != null) {
						return inputConverter.revert(value);
					}

					return dataObject.getProperty(key);
				}

				return "";
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		if (inJavaScriptContext) {
			return ERROR_MESSAGE_GET_OR_NULL_JS;
		}

		return ERROR_MESSAGE_GET_OR_NULL;
	}

	@Override
	public String shortDescription() {
		return "Returns the value with the given name of the given entity, or null";
	}

}
