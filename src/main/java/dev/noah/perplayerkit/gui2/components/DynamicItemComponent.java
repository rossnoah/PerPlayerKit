/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.components;

public class DynamicItemComponent extends BaseComponent {
    public DynamicItemComponent() { super("dynamic_item"); }
}

class ProgressBarComponent extends BaseComponent {
    public ProgressBarComponent() { super("progress_bar"); }
}

class ListComponent extends BaseComponent {
    public ListComponent(dev.noah.perplayerkit.gui2.core.GuiManager manager) { super("list"); }
}

class BorderComponent extends BaseComponent {
    public BorderComponent() { super("border"); }
}