package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.glfw.GLFW;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserUrlScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserUrlScreen.class);
    private final BlockEntity entity;
    private EditBox urlField;
    private EditBox inputField;

    public BrowserUrlScreen(BlockEntity entity) {
        super(Component.translatable("gui.mcef-addon.browser_control.title"));
        this.entity = entity;
        LOGGER.info("BrowserUrlScreen constructor called with entity: {}", entity);
    }

    private String getEntityUrl() {
        if (entity instanceof BrowserScreenBlockEntity bbe) return bbe.getUrl();
        if (entity instanceof BrowserBlockEntity bbe) return bbe.getUrl();
        if (entity instanceof BrowserComputerBlockEntity cbe) return cbe.getUrl();
        return "";
    }

    private void setEntityUrl(String url) {
        if (entity instanceof BrowserScreenBlockEntity bbe) bbe.setUrl(url);
        if (entity instanceof BrowserBlockEntity bbe) bbe.setUrl(url);
        if (entity instanceof BrowserComputerBlockEntity cbe) cbe.setUrl(url);
    }

    private IMCEFBrowser getBrowser() {
        if (entity instanceof BrowserScreenBlockEntity bbe) return bbe.getBrowser();
        return null;
    }

    @Override
    protected void init() {
        int w = 200;
        int h = 20;
        int x = (width - w) / 2;
        int y = height / 2 - 40;

        urlField = new EditBox(font, x, y, w, h, Component.translatable("gui.mcef-addon.browser_control.url"));
        urlField.setMaxLength(2048);
        urlField.setValue(getEntityUrl());
        urlField.setHint(Component.translatable("gui.mcef-addon.browser_control.url"));
        addRenderableWidget(urlField);

        addRenderableWidget(Button.builder(Component.translatable("gui.mcef-addon.browser_control.set_url"), button -> {
            setEntityUrl(urlField.getValue());
        }).bounds(x, y + 25, w, h).build());

        // Only show keyboard input if it's a screen
        if (entity instanceof BrowserScreenBlockEntity) {
            y += 60;
            inputField = new EditBox(font, x, y, w, h, Component.translatable("gui.mcef-addon.browser_control.send_keys"));
            inputField.setMaxLength(256);
            inputField.setHint(Component.translatable("gui.mcef-addon.browser_control.send_keys"));
            addRenderableWidget(inputField);

            addRenderableWidget(Button.builder(Component.translatable("gui.mcef-addon.browser_control.send_text"), button -> {
                sendTextToBrowser(inputField.getValue());
                inputField.setValue("");
            }).bounds(x, y + 25, w, h).build());
        }
    }

    private void sendTextToBrowser(String text) {
        IMCEFBrowser browser = getBrowser();
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
        super.render(guiGraphics, mouseX, mouseY, delta);
        
        // Draw Title
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        
        // Draw Labels
        if (inputField != null) {
            guiGraphics.drawString(font, Component.translatable("gui.mcef-addon.browser_control.keys_label"), (width - 200) / 2, height / 2 + 8, 0xAAAAAA);
        }
        guiGraphics.drawString(font, Component.translatable("gui.mcef-addon.browser_control.url_label"), (width - 200) / 2, height / 2 - 52, 0xAAAAAA);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(guiGraphics);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (urlField.isFocused()) {
                setEntityUrl(urlField.getValue());
                return true;
            } else if (inputField != null && inputField.isFocused()) {
                sendTextToBrowser(inputField.getValue());
                inputField.setValue("");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
