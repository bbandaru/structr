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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.mozilla.javascript.ScriptableObject;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DateFormatFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DATE_FORMAT    = "Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";
	public static final String ERROR_MESSAGE_DATE_FORMAT_JS = "Usage: ${{Structr.date_format(value, pattern)}}. Example: ${{Structr.date_format(Structr.get('this').creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}}";

	@Override
	public String getName() {
		return "date_format()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 2 || sources[1] == null) {
			logParameterError(entity, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
		
		try {
			if (!arrayHasLengthAndAllElementsNotNull(sources, 2)) {
				
				return "";
			}

			Date date = null;

			if (sources[0] instanceof Date) {

				date = (Date)sources[0];

			} else if (sources[0] instanceof Number) {

				date = new Date(((Number)sources[0]).longValue());

			} else if (sources[0] instanceof ScriptableObject) {
				// FIXME: This was empty - what is it here for?
			} else {

				try {

					// parse with format from IS
					date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

				} catch (ParseException ex) {

					logException(entity, ex, sources);

				}

			}

			// format with given pattern
			return new SimpleDateFormat(sources[1].toString()).format(date);
			
		} catch (final IllegalArgumentException e) {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DATE_FORMAT_JS : ERROR_MESSAGE_DATE_FORMAT);
	}

	@Override
	public String shortDescription() {
		return "Formats the given value as a date string with the given format string";
	}

}
