/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.dropdown;

import java.util.HashSet;
import java.util.Set;

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.PKeyCodes;

public abstract class DropDownContainer<V, C extends DropDownContainerConfiguration> implements IsPWidget {

    protected static final String STRING_SPACE = " ";

    protected static final String STYLE_CONTAINER_BUTTON_PLACEHOLDER = "dd-container-button-placeholder";
    protected static final String STYLE_CONTAINER_SELECTED = "dd-container-selected";

    private static final String STYLE_CONTAINER_WIDGET = "dd-container-widget";
    private static final String STYLE_CONTAINER_DISABLED = "dd-container-disabled";
    private static final String STYLE_CONTAINER_BUTTON = "dd-container-button";
    private static final String STYLE_CONTAINER_STATE = "dd-container-state";
    private static final String STYLE_CONTAINER_CLEAR = "dd-container-clear";
    private static final String STYLE_CONTAINER_CLEAR_DISABLED = "dd-container-clear-disabled";
    private static final String STYLE_CONTAINER_OPENED = "dd-container-opened";
    private static final String STYLE_CONTAINER_WIDGET_OPENED = "dd-container-widget-opened";
    private static final String STYLE_CONTAINER_ADDON = "dd-container-addon";
    private static final String STYLE_CONTAINER_CUSTOM = "dd-container-custom";
    private static final String STYLE_CONTAINER_DEFAULT = "dd-container-default";

    private static final String ATTRIBUTE_ID = "id";

    protected PButton clearTitleButton;
    protected PButton mainButton;
    protected PButton stateButton;

    private IsPWidget customContainer;

    private boolean initialized;

    protected final C configuration;

    private final PFlowPanel widget;
    private final DropDownContainerAddon container;
    private final Set<PValueChangeHandler<V>> valueChangeHandlers;
    private final Set<PCloseHandler> closeHandlers;
    private final Set<POpenHandler> openHandlers;
    private final Set<DropDownContainerListener> listeners;

    public DropDownContainer(final C configuration) {
        this.configuration = configuration;
        this.valueChangeHandlers = new HashSet<>();
        this.closeHandlers = new HashSet<>();
        this.openHandlers = new HashSet<>();
        this.listeners = new HashSet<>();
        this.widget = Element.newPFlowPanel();
        this.widget.addStyleName(STYLE_CONTAINER_WIDGET);
        this.widget.setAttribute(ATTRIBUTE_ID, widget.getID() + "");
        this.container = new DropDownContainerAddon(widget);
        this.container.addStyleName(STYLE_CONTAINER_ADDON);
    }

    public abstract V getValue();

    public abstract void setValue(V value);

    private boolean focused;

    @Override
    public PFlowPanel asWidget() {
        if (!initialized) {
            widget.setTabindex(TabindexMode.TABULABLE);
            widget.stopEvent(PEventType.KEYEVENTS);
            widget.addDomHandler((PFocusHandler) event -> {
                focused = true;
                onFocus();
            }, PFocusEvent.TYPE);
            widget.addDomHandler((PBlurHandler) event -> {
                focused = false;
                onBlur();
            }, PBlurEvent.TYPE);
            widget.addKeyUpHandler(e -> {
                if (focused && e.getKeyCode() == PKeyCodes.ENTER.getCode()) {
                    setContainerVisible(!container.isVisible());
                } else if (e.getKeyCode() == PKeyCodes.ESCAPE.getCode()) {
                    close();
                }
            });
            mainButton = Element.newPButton(configuration.getTitle());
            mainButton.addStyleName(STYLE_CONTAINER_BUTTON);
            mainButton.setTabindex(TabindexMode.FOCUSABLE);
            if (configuration.isTitleDisplayed() && configuration.isTitlePlaceHolder()) {
                mainButton.addStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
            }
            widget.add(mainButton);

            stateButton = Element.newPButton();
            stateButton.addStyleName(STYLE_CONTAINER_STATE);
            stateButton.setTabindex(TabindexMode.FOCUSABLE);
            widget.add(stateButton);

            if (configuration.isClearTitleButtonEnabled()) {
                clearTitleButton = Element.newPButton();
                clearTitleButton.addStyleName(STYLE_CONTAINER_CLEAR);
                clearTitleButton.setTitle(configuration.getClearLabel());
                clearTitleButton.addClickHandler(e -> {
                    if (isEnabled()) {
                        setValue(null);
                        listeners.forEach(l -> l.onClearTitleClicked());
                        onValueChange();
                    }
                });
                widget.add(clearTitleButton);
                setClearTitleButtonVisible(false);
            } else {
                widget.addStyleName(STYLE_CONTAINER_CLEAR_DISABLED);
            }

            if(!configuration.isEventOnlyEnabled()){
                widget.addInitializeListener(e -> {
                    updateTitle(getValue());
                });
            }

            if (customContainer != null) {
                container.add(customContainer);
                customContainer.asWidget().addStyleName(STYLE_CONTAINER_CUSTOM);
            }

            final PWidget defaultContainer = createDefaultContainer();
            defaultContainer.addStyleName(STYLE_CONTAINER_DEFAULT);
            container.add(defaultContainer);

            final PClickHandler clickHandler = e -> {
                setContainerVisible(!container.isVisible());
            };
            mainButton.addClickHandler(clickHandler);
            stateButton.addClickHandler(clickHandler);
            container.addListener(() -> setContainerVisible(false));
            initialized = true;
        }
        widget.addDestroyListener(e -> {
            container.asWidget().removeFromParent();
            valueChangeHandlers.clear();
            closeHandlers.clear();
            openHandlers.clear();
            listeners.clear();
        });
        return widget;
    }

    public void focus() {
        widget.focus();
    }

    public void blur() {
        widget.blur();
    }

    public C getConfiguration() {
        return configuration;
    }

    public void open() {
        setContainerVisible(true);
    }

    public void close() {
        setContainerVisible(false);
    }

    public void updateContainerPosition() {
        container.updatePosition();
    }

    public void disableSpaceWhenOpened() {
        container.disableSpaceWhenOpened();
    }

    public void forceUpdateTitle() {
        updateTitle(getValue());
    }

    public boolean isEnabled() {
        return !widget.hasStyleName(STYLE_CONTAINER_DISABLED);
    }

    public void setEnabled(final boolean enabled) {
        if (enabled) {
            widget.removeStyleName(STYLE_CONTAINER_DISABLED);
            widget.setTabindex(TabindexMode.TABULABLE);
        } else {
            widget.addStyleName(STYLE_CONTAINER_DISABLED);
            widget.setTabindex(TabindexMode.FOCUSABLE);
        }
    }

    public void setClearTitleButtonVisible(final boolean visible) {
        if (clearTitleButton != null) clearTitleButton.setVisible(visible);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isOpen() {
        return widget.hasStyleName(STYLE_CONTAINER_OPENED);
    }

    public boolean isFocused() {
        return focused;
    }

    public void addStyleName(final String styleName) {
        widget.addStyleName(styleName);
    }

    public void removeStyleName(final String styleName) {
        widget.removeStyleName(styleName);
    }

    public void addContainerStyleName(final String styleName) {
        container.addStyleName(styleName);
    }

    public void setCustomContainer(final IsPWidget customContainer) {
        if (initialized) throw new IllegalArgumentException("Must be set before asWidget call");
        this.customContainer = customContainer;
    }

    public void setDefaultContainerEnabled(final boolean enabled) {
        // Nothing to do by default
    }

    public void addValueChangeHandler(final PValueChangeHandler<V> handler) {
        valueChangeHandlers.add(handler);
    }

    public void removeValueChangeHandler(final PValueChangeHandler<V> handler) {
        valueChangeHandlers.remove(handler);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        closeHandlers.add(handler);
    }

    public void removeCloseHandler(final PCloseHandler handler) {
        closeHandlers.remove(handler);
    }

    public void addOpenHandler(final POpenHandler handler) {
        openHandlers.add(handler);
    }

    public void removeOpenHandler(final POpenHandler handler) {
        openHandlers.remove(handler);
    }


    public void addListener(final DropDownContainerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DropDownContainerListener listener) {
        listeners.remove(listener);
    }

    protected abstract PWidget createDefaultContainer();

    protected abstract boolean isValueEmpty(V value);

    protected void updateTitle(final V value) {
        if (!isInitialized()) return;
        if(configuration.isEventOnlyEnabled()) return;
        final StringBuilder text = new StringBuilder();
        if (configuration.isTitleDisplayed()) {
            if (configuration.isTitlePlaceHolder()) {
                if (value == null || isValueEmpty(value)) {
                    mainButton.addStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                    text.append(configuration.getTitle());
                } else {
                    mainButton.removeStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                }
            } else {
                text.append(configuration.getTitle());
                text.append(STRING_SPACE);
                text.append(configuration.getTitleSeparator());
                text.append(STRING_SPACE);
            }
        }
        if (value == null || isValueEmpty(value)) {
            if (!configuration.isTitlePlaceHolder() && configuration.isSelectionDisplayed()) text.append(configuration.getAllLabel());
            final String display = text.toString();
            mainButton.setText(display);
            mainButton.setTitle(display);
            stateButton.setTitle(display);
            widget.removeStyleName(STYLE_CONTAINER_SELECTED);
        } else {
            if (configuration.isSelectionDisplayed()) appendDisplayValue(text, value);
            final String display = text.toString();
            mainButton.setText(display);
            mainButton.setTitle(display);
            stateButton.setTitle(display);
            widget.addStyleName(STYLE_CONTAINER_SELECTED);
        }
        setClearTitleButtonVisible(value != null && !isValueEmpty(value));
    }

    protected void appendDisplayValue(final StringBuilder text, final V value) {
        text.append(value.toString());
    }

    protected void onValueChange() {
        onValueChange(getValue());
    }


    protected void onValueChange(V value) {
        final PValueChangeEvent<V> event = new PValueChangeEvent<>(this, value);
        valueChangeHandlers.forEach(l -> l.onValueChange(event));
    }

    protected void beforeContainerVisible() {
        // Nothing to do by default
    }

    protected void afterContainerVisible() {
        // Nothing to do by default
    }

    protected void afterContainerClose() {
        // Nothing to do by default
    }

    protected void onFocus() {
        // Nothing to do by default
    }

    protected void focusContainer() {
        // Nothing to do by default
    }

    protected void onBlur() {
        if (isOpen() && isContainerFocusable()) {
            focusContainer();
        } else {
            close();
        }
    }

    protected boolean isContainerFocusable() {
        return false;
    }

    private void setContainerVisible(final boolean visible) {
        if (!isEnabled()) return;
        if (visible && !isOpen()) {
            widget.addStyleName(STYLE_CONTAINER_OPENED);
            container.addStyleName(STYLE_CONTAINER_WIDGET_OPENED);
            beforeContainerVisible();
            container.show();
            afterContainerVisible();
            final POpenEvent event = new POpenEvent(this);
            openHandlers.forEach(l -> l.onOpen(event));
        } else if (!visible && isOpen()) {
            widget.removeStyleName(STYLE_CONTAINER_OPENED);
            container.removeStyleName(STYLE_CONTAINER_WIDGET_OPENED);
            container.hide();
            updateTitle(getValue());
            afterContainerClose();
            final PCloseEvent event = new PCloseEvent(this);
            closeHandlers.forEach(l -> l.onClose(event));
        }
    }

    //

    public interface DropDownContainerListener {

        void onClearTitleClicked();

    }
}
