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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DivFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DIV = "Usage: ${div(value1, value2)}. Example: ${div(5, 2)}";

	@Override
	public String getName() {
		return "div()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		try {

			if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

				try {

					return Long.parseLong(sources[0].toString()) / Long.parseLong(sources[1].toString());

				} catch (NumberFormatException nfe) {

					logException(nfe, "{}: NumberFormatException in element \"{}\" for parameters: {}", new Object[] { getName(), entity, getParametersAsString(sources) });
					return nfe.getMessage();

				}

			} else if (sources.length > 0 && sources[0] != null) {

				try {

					return Long.parseLong(sources[0].toString());

				} catch (NumberFormatException nfe) {

					logException(nfe, "{}: NumberFormatException in element \"{}\" for parameters: {}", new Object[] { getName(), entity, getParametersAsString(sources) });
					return nfe.getMessage();
				}

			}

		} catch (final IllegalArgumentException e) {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return null;

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_DIV;
	}

	@Override
	public String shortDescription() {
		return "Integer division, first argument / second argument";
	}

}