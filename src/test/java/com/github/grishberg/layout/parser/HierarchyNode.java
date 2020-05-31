package com.github.grishberg.layout.parser;

import com.android.layoutinspector.model.ViewNode;

import java.util.Enumeration;

public class HierarchyNode implements Node {
    private final Node parent;
    private final ViewNode viewNode;

    public HierarchyNode(Node parent, ViewNode viewNode) {
        this.parent = parent;
        this.viewNode = viewNode;
    }

    @Override
    public int childCount() {
        return viewNode.getChildCount();
    }

    @Override
    public Enumeration<Node> children() {
        return new ChildrenEnumeration(null, viewNode.children());
    }

    @Override
    public Node childAt(int index) {
        return new HierarchyNode(parent, viewNode.getChildAt(index));
    }

    @Override
    public String name() {
        return viewNode.getName();
    }

    @Override
    public NodeProperty getProperty(String name, String altNames) {
        return new ViewNodeProperty(viewNode.getProperty(name, altNames));
    }

    @Override
    public Node parent() {
        return parent;
    }

    private static class ChildrenEnumeration implements Enumeration<Node> {

        private final Node parent;
        private final Enumeration<?> children;

        ChildrenEnumeration(Node parent, Enumeration<?> children) {
            this.parent = parent;

            this.children = children;
        }

        @Override
        public boolean hasMoreElements() {
            return children.hasMoreElements();
        }

        @Override
        public Node nextElement() {
            return new HierarchyNode(parent, (ViewNode) children.nextElement());
        }
    }
}