/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.web.entity.relation.ContainerContentItems;

/**
 * Base class for all content items.
 */
public abstract class ContentItem extends AbstractNode {

	public static final Property<List<ContentContainer>>   containers    = new StartNodes<>("containers", ContainerContentItems.class);
	public static final Property<Boolean>                  isContentItem = new ConstantBooleanProperty("isContentItem", true);

	public static final View publicView = new View(Folder.class, PropertyView.Public, id, type, name, owner, containers, isContentItem);
	public static final View uiView     = new View(Folder.class, PropertyView.Ui, id, type, name, owner, containers, isContentItem);
	
}
