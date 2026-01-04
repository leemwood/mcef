package com.cinemamod.mcef;

/**
 * A wrapper class for the browser to maintain backward compatibility.
 * It delegates all calls to the platform-specific implementation.
 */
public class MCEFBrowser implements IMCEFBrowser {

    private final IMCEFBrowser delegate;

    public MCEFBrowser(IMCEFBrowser delegate) {
        this.delegate = delegate;
    }

    public MCEFBrowser(String url, boolean transparent) {
        MCEFPlatform platform = MCEFPlatform.getPlatform();
        if (platform.isAndroid()) {
            this.delegate = new AndroidMCEFBrowser(url, transparent);
        } else {
            MCEF.getLogger().error("JCEF is no longer supported on this platform.");
            this.delegate = null; // Or a dummy implementation
        }
    }

    @Override
    public void loadURL(String url) {
        if (delegate != null) delegate.loadURL(url);
    }

    @Override
    public void resize(int width, int height) {
        if (delegate != null) delegate.resize(width, height);
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public MCEFRenderer getRenderer() {
        return delegate != null ? delegate.getRenderer() : null;
    }

    @Override
    public void sendMouseMove(int x, int y, int modifiers) {
        if (delegate != null) delegate.sendMouseMove(x, y, modifiers);
    }

    @Override
    public void sendMousePress(int x, int y, int modifiers, int button, boolean isRelease, int clickCount) {
        if (delegate != null) delegate.sendMousePress(x, y, modifiers, button, isRelease, clickCount);
    }

    @Override
    public void sendMouseWheel(int x, int y, int modifiers, int delta) {
        if (delegate != null) delegate.sendMouseWheel(x, y, modifiers, delta);
    }

    @Override
    public void sendKeyPress(int key, char character, int modifiers) {
        if (delegate != null) delegate.sendKeyPress(key, character, modifiers);
    }

    @Override
    public void sendKeyRelease(int key, char character, int modifiers) {
        if (delegate != null) delegate.sendKeyRelease(key, character, modifiers);
    }

    @Override
    public void sendKeyType(int key, char character, int modifiers) {
        if (delegate != null) delegate.sendKeyType(key, character, modifiers);
    }

    @Override
    public void setFocus(boolean focus) {
        if (delegate != null) delegate.setFocus(focus);
    }

    @Override
    public void runJS(String script, String url) {
        if (delegate != null) delegate.runJS(script, url);
    }

    @Override
    public String getURL() {
        return delegate != null ? delegate.getURL() : "";
    }

    @Override
    public boolean canGoBack() {
        return delegate != null && delegate.canGoBack();
    }

    @Override
    public boolean canGoForward() {
        return delegate != null && delegate.canGoForward();
    }

    @Override
    public void goBack() {
        if (delegate != null) delegate.goBack();
    }

    @Override
    public void goForward() {
        if (delegate != null) delegate.goForward();
    }

    @Override
    public void reload() {
        if (delegate != null) delegate.reload();
    }

    @Override
    public void reloadIgnoreCache() {
        if (delegate != null) delegate.reloadIgnoreCache();
    }

    @Override
    public void stopLoad() {
        if (delegate != null) delegate.stopLoad();
    }

    @Override
    public boolean isTransparent() {
        return delegate != null && delegate.isTransparent();
    }

    // Add methods that were previously in MCEFBrowser but not in IMCEFBrowser for compatibility
    // These might be JCEF specific, we might need to handle them carefully.
}
