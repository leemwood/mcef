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

    @Override
    public void loadURL(String url) {
        delegate.loadURL(url);
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public MCEFRenderer getRenderer() {
        return delegate.getRenderer();
    }

    @Override
    public void sendMouseMove(int x, int y, int modifiers) {
        delegate.sendMouseMove(x, y, modifiers);
    }

    @Override
    public void sendMousePress(int x, int y, int modifiers, int button, boolean isRelease, int clickCount) {
        delegate.sendMousePress(x, y, modifiers, button, isRelease, clickCount);
    }

    @Override
    public void sendMouseWheel(int x, int y, int modifiers, int delta) {
        delegate.sendMouseWheel(x, y, modifiers, delta);
    }

    @Override
    public void sendKeyPress(int key, char character, int modifiers) {
        delegate.sendKeyPress(key, character, modifiers);
    }

    @Override
    public void sendKeyRelease(int key, char character, int modifiers) {
        delegate.sendKeyRelease(key, character, modifiers);
    }

    @Override
    public void sendKeyType(int key, char character, int modifiers) {
        delegate.sendKeyType(key, character, modifiers);
    }

    @Override
    public void setFocus(boolean focus) {
        delegate.setFocus(focus);
    }

    @Override
    public void runJS(String script, String url) {
        delegate.runJS(script, url);
    }

    @Override
    public String getURL() {
        return delegate.getURL();
    }

    @Override
    public boolean canGoBack() {
        return delegate.canGoBack();
    }

    @Override
    public boolean canGoForward() {
        return delegate.canGoForward();
    }

    @Override
    public void goBack() {
        delegate.goBack();
    }

    @Override
    public void goForward() {
        delegate.goForward();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public void reloadIgnoreCache() {
        delegate.reloadIgnoreCache();
    }

    @Override
    public void stopLoad() {
        delegate.stopLoad();
    }

    @Override
    public boolean isTransparent() {
        return delegate.isTransparent();
    }

    // Add methods that were previously in MCEFBrowser but not in IMCEFBrowser for compatibility
    // These might be JCEF specific, we might need to handle them carefully.
}
