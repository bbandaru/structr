/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.graph;

import java.util.Iterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Sets the properties found in the property set on all relationships matching the type.
 * If no type property is found, set the properties on all relationships.
 *
 * @author Axel Morgner
 */
public class BulkSetRelationshipPropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetRelationshipPropertiesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final GraphDatabaseService graphDb            = (GraphDatabaseService) arguments.get("graphDb");
		final RelationshipFactory relationshipFactory = new RelationshipFactory(securityContext);

		if (graphDb != null) {

			Iterator<AbstractRelationship> relIterator = null;
			final String typeName                      = "type";

			if (properties.containsKey(typeName)) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					relIterator = StructrApp.getInstance(securityContext).relationshipQuery(SchemaHelper.getEntityClassForRawType(typeName)).getAsList().iterator();
					tx.success();
				}

				properties.remove(typeName);

			} else {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					relIterator = Iterables.map(relationshipFactory, GlobalGraphOperations.at(graphDb).getAllRelationships()).iterator();
					tx.success();
				}
			}

			final AtomicLong count = new AtomicLong();
			NodeServiceCommand.bulkGraphOperation(securityContext, relIterator, 1000, "SetRelationshipProperties", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					// Treat only "our" nodes
					if (rel.getProperty(AbstractRelationship.id) != null) {

						for (Entry entry : properties.entrySet()) {

							String key = (String) entry.getKey();
							Object val = entry.getValue();

							PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(rel.getClass(), key);
							if (propertyKey != null) {

								try {
									rel.setProperty(propertyKey, val);


								} catch (FrameworkException fex) {

									logger.log(Level.WARNING, "Unable to set relationship property {0} of relationship {1} to {2}: {3}", new Object[] { propertyKey, rel.getUuid(), val, fex.getMessage() } );
								}
							}
						}
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.log(Level.WARNING, "Unable to set properties of relationship {0}: {1}", new Object[] { rel.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set relationship properties: {0}", t.getMessage() );
				}

				@Override
				public Predicate<Long> getCondition() {
					return null;
				}

				@Override
				public AtomicLong getCounter() {
					return count;
				}
			});

			logger.log(Level.INFO, "Finished setting properties on {0} relationships", count.get());
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
