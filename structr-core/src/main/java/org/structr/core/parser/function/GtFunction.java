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
package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GtFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GT = "Usage: ${gt(value1, value2)}. Example: ${if(gt(this.children, 2), \"More than two\", \"Equal to or less than two\")}";

	@Override
	public String getName() {
		return "gt()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return gt(sources[0], sources[1]);
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GT;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the first argument is greater than the second argument";
	}

}