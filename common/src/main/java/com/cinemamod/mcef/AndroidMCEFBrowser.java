package com.cinemamod.mcef;

import com.cinemamod.mcef.internal.AndroidBridge;
import net.minecraft.client.Minecraft;

public class AndroidMCEFBrowser implements IMCEFBrowser {

    private final MCEFRenderer renderer;
    private String url;
    private boolean transparent;
    private int width, height;

    public AndroidMCEFBrowser(String url, boolean transparent) {
        this.url = url;
        this.transparent = transparent;
        this.renderer = new MCEFRenderer(transparent);
        
        Minecraft.getInstance().submit(renderer::initialize);
        
        // TODO: Call Android side to create WebView and associate with renderer's texture
    }

    @Override
    public void loadURL(String url) {
        this.url = url;
        // TODO: Call Android side to load URL
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        // TODO: Call Android side to resize WebView
    }

    @Override
    public void close() {
        // TODO: Call Android side to destroy WebView
    }

    @Override
    public MCEFRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void sendMouseMove(int x, int y, int modifiers) {
        // TODO: Map to Android motion event
    }

    @Override
    public void sendMousePress(int x, int y, int modifiers, int button, boolean isRelease, int clickCount) {
        // TODO: Map to Android motion event
    }

    @Override
    public void sendMouseWheel(int x, int y, int modifiers, int delta) {
        // TODO: Map to Android motion event
    }

    @Override
    public void sendKeyPress(int key, char character, int modifiers) {
        // TODO: Map to Android key event
    }

    @Override
    public void sendKeyRelease(int key, char character, int modifiers) {
        // TODO: Map to Android key event
    }

    @Override
    public void sendKeyType(int key, char character, int modifiers) {
        // TODO: Map to Android key event
    }

    @Override
    public void runJS(String script, String url) {
        // TODO: Call Android side to evaluate JS
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public boolean canGoBack() {
        return false; // TODO
    }

    @Override
    public boolean canGoForward() {
        return false; // TODO
    }

    @Override
    public void goBack() {
        // TODO
    }

    @Override
    public void goForward() {
        // TODO
    }

    @Override
    public void reload() {
        // TODO
    }

    @Override
    public void reloadIgnoreCache() {
        // TODO
    }

    @Override
    public void stopLoad() {
        // TODO
    }

    @Override
    public void setFocus(boolean focus) {
        // TODO
    }

    @Override
    public boolean isTransparent() {
        return transparent;
    }
}
