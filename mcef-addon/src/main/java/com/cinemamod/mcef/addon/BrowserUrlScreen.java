package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BrowserUrlScreen extends Screen {
    private final BrowserBlockEntity entity;
    private EditBox urlField;
    private EditBox inputField;

    public BrowserUrlScreen(BrowserBlockEntity entity) {
        super(Component.literal("Browser Control"));
        this.entity = entity;
    }

    @Override
    protected void init() {
        int w = 200;
        int h = 20;
        int x = (width - w) / 2;
        int y = height / 2 - 40;

        urlField = new EditBox(font, x, y, w, h, Component.literal("URL"));
        urlField.setMaxLength(2048);
        urlField.setValue(entity.getUrl());
        addWidget(urlField);

        addRenderableWidget(Button.builder(Component.literal("Set URL"), button -> {
            entity.setUrl(urlField.getValue());
        }).bounds(x, y + 25, w, h).build());

        y += 60;
        inputField = new EditBox(font, x, y, w, h, Component.literal("Send Keys"));
        inputField.setMaxLength(256);
        addWidget(inputField);

        addRenderableWidget(Button.builder(Component.literal("Send Text"), button -> {
            sendTextToBrowser(inputField.getValue());
            inputField.setValue("");
        }).bounds(x, y + 25, w, h).build());
    }

    private void sendTextToBrowser(String text) {
        IMCEFBrowser browser = entity.getBrowser();
        if (browser != null) {
            for (char c : text.toCharArray()) {
                browser.sendKeyTyped(c, 0);
            }
            browser.sendKeyPress(GLFW.GLFW_KEY_ENTER, 0, 0);
            browser.sendKeyRelease(GLFW.GLFW_KEY_ENTER, 0, 0);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        urlField.render(guiGraphics, mouseX, mouseY, delta);
        inputField.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawString(font, "Browser URL:", (width - 200) / 2, height / 2 - 52, 0xFFFFFF);
        guiGraphics.drawString(font, "Simulate Keyboard:", (width - 200) / 2, height / 2 + 8, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (urlField.isFocused()) {
                entity.setUrl(urlField.getValue());
                return true;
            } else if (inputField.isFocused()) {
                sendTextToBrowser(inputField.getValue());
                inputField.setValue("");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
