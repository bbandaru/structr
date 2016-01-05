package org.structr.function;

import java.util.Date;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.rest.logging.entity.LogEvent;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class LogEventFunction extends UiFunction {

	public static final String ERROR_MESSAGE_LOG_EVENT    = "Usage: ${log_event(action, message)}. Example: ${log_event('read', 'Book has been read')}";
	public static final String ERROR_MESSAGE_LOG_EVENT_JS = "Usage: ${{Structr.logEvent(action, message)}}. Example: ${{Structr.logEvent('read', 'Book has been read')}}";

	@Override
	public String getName() {
		return "log_event()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			final String action = sources[0].toString();
			final String message = sources[1].toString();

			final LogEvent logEvent = StructrApp.getInstance().create(LogEvent.class,
				new NodeAttribute(LogEvent.actionProperty, action),
				new NodeAttribute(LogEvent.messageProperty, message),
				new NodeAttribute(LogEvent.timestampProperty, new Date())
			);

			switch (sources.length) {

				case 4:
					final String object = sources[3].toString();
					logEvent.setProperty(LogEvent.objectProperty, object);
					// no break, next case should be included

				case 3:
					final String subject = sources[2].toString();
					logEvent.setProperty(LogEvent.subjectProperty, subject);
					break;
			}

			return logEvent;

		} else if (sources.length == 1 && sources[0] instanceof Map) {

			// support javascript objects here
			final Map map = (Map)sources[0];
			final String action = DOMNode.objectToString(map.get("action"));
			final String message = DOMNode.objectToString(map.get("message"));
			final String subject = DOMNode.objectToString(map.get("subject"));
			final String object = DOMNode.objectToString(map.get("object"));

			return StructrApp.getInstance().create(LogEvent.class,
				new NodeAttribute(LogEvent.actionProperty, action),
				new NodeAttribute(LogEvent.messageProperty, message),
				new NodeAttribute(LogEvent.timestampProperty, new Date()),
				new NodeAttribute(LogEvent.subjectProperty, subject),
				new NodeAttribute(LogEvent.objectProperty, object)
			);
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOG_EVENT_JS : ERROR_MESSAGE_LOG_EVENT);
	}

	@Override
	public String shortDescription() {
		return "Logs an event to the Structr log";
	}
}
