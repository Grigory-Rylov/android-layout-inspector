package com.github.grishberg.layout.parser;

import com.android.layoutinspector.model.ViewProperty;

public class ViewNodeProperty implements NodeProperty {
    private final ViewProperty property;

    public ViewNodeProperty(ViewProperty property) {
        this.property = property;
    }

    @Override
    public String fullName() {
        return property.getFullName();
    }

    @Override
    public String name() {
        return property.getName();
    }

    @Override
    public String category() {
        return property.getCategory();
    }

    @Override
    public String value() {
        return property.getValue();
    }
}
