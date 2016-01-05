package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class PropertyInfoFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_PROPERTY_INFO    = "Usage: ${property_info(type, name)}. Example ${property_info('User', 'name')}";
	public static final String ERROR_MESSAGE_PROPERTY_INFO_JS = "Usage: ${Structr.propertyInfo(type, name)}. Example ${Structr.propertyInfo('User', 'name')}";

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final String typeName = sources[0].toString();
			final String keyName = sources[1].toString();

			Class type = config.getNodeEntityClass(typeName);
			if (type == null) {

				type = config.getRelationshipEntityClass(typeName);
			}

			if (type != null) {

				final PropertyKey key = config.getPropertyKeyForJSONName(type, keyName, false);
				if (key != null) {

					return SchemaHelper.getPropertyInfo(ctx.getSecurityContext(), key);

				} else {

					return "Unknown property " + typeName + "." + keyName;
				}

			} else {

				return "Unknown type " + typeName;
			}

		} else {

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PROPERTY_INFO_JS : ERROR_MESSAGE_PROPERTY_INFO);
	}

	@Override
	public String shortDescription() {
		return "Returns the schema information for the given property";
	}


	@Override
	public String getName() {
		return "schema_property()";
	}


}
