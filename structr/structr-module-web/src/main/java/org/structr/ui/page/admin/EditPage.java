/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.click.control.FieldSet;
import org.structr.core.entity.StructrNode;

import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.dataprovider.DataProvider;
import org.structr.common.SearchOperator;
import org.structr.core.entity.web.Page;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.Template;
import org.structr.core.node.SearchAttribute;
import org.structr.core.node.SearchNodeCommand;

/**
 * Edit text.
 * 
 * @author amorgner
 */
public class EditPage extends DefaultEdit {

    protected Page page;
    protected Select templateSelect = new Select(Page.TEMPLATE_KEY);

    public EditPage() {

        super();

        FieldSet templateFields = new FieldSet("Template");
        templateFields.add(templateSelect);

        editPropertiesForm.add(templateFields);
    }

    @Override
    public void onInit() {

        super.onInit();

        page = (Page) node;

        if (page == null) {
            return;
        }

        final Template templateNode = page.getTemplate(user);

        templateSelect.setDataProvider(new DataProvider() {

            @Override
            public List<Option> getData() {
                List<Option> options = new ArrayList<Option>();
                List<StructrNode> nodes = null;
                if (templateNode != null) {
                    nodes = templateNode.getSiblingNodes(user);
                } else {
                    Command searchNode = Services.command(SearchNodeCommand.class);

                    List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();
                    searchAttrs.add(new SearchAttribute(StructrNode.TYPE_KEY, Template.class.getSimpleName(), SearchOperator.OR));
                    nodes = (List<StructrNode>) searchNode.execute(null, null, true, false, searchAttrs);
                }
                if (nodes != null) {
                    Collections.sort(nodes);
                    options.add(Option.EMPTY_OPTION);
                    for (StructrNode n : nodes) {
                        if (n instanceof Template) {
                            Option opt = new Option(n.getId(), n.getName());
                            options.add(opt);
                        }
                    }
                }
                return options;
            }
        });

        externalViewUrl = node.getNodeURL(user, contextPath);
        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(node.getNodePath(user).replace("&", "%26")));

        // render node's default view
        StringBuilder out = new StringBuilder();
        node.renderView(out, node, null, null, user);
        rendition = out.toString();

    }
}
