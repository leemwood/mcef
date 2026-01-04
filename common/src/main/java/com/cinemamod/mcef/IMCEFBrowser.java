package com.cinemamod.mcef;

public interface IMCEFBrowser {

    void loadURL(String url);

    void resize(int width, int height);

    void close();

    MCEFRenderer getRenderer();

    void sendMouseMove(int x, int y, int modifiers);

    default void sendMouseMove(int x, int y) {
        sendMouseMove(x, y, 0);
    }

    void sendMousePress(int x, int y, int modifiers, int button, boolean isRelease, int clickCount);

    default void sendMousePress(int x, int y, int button) {
        sendMousePress(x, y, 0, button, false, 1);
    }

    default void sendMouseRelease(int x, int y, int button) {
        sendMousePress(x, y, 0, button, true, 1);
    }

    void sendMouseWheel(int x, int y, int modifiers, int delta);

    default void sendMouseWheel(int x, int y, double scrollY, int modifiers) {
        sendMouseWheel(x, y, modifiers, (int) scrollY);
    }

    void sendKeyPress(int key, char character, int modifiers);

    default void sendKeyPress(int key, int scanCode, int modifiers) {
        sendKeyPress(key, (char) 0, modifiers);
    }

    void sendKeyRelease(int key, char character, int modifiers);

    default void sendKeyRelease(int key, int scanCode, int modifiers) {
        sendKeyRelease(key, (char) 0, modifiers);
    }

    void sendKeyType(int key, char character, int modifiers);

    default void sendKeyTyped(char character, int modifiers) {
        sendKeyType(0, character, modifiers);
    }

    void setFocus(boolean focus);

    void runJS(String script, String url);

    String getURL();

    boolean canGoBack();

    boolean canGoForward();

    void goBack();

    void goForward();

    void reload();

    void reloadIgnoreCache();

    void stopLoad();

    boolean isTransparent();
}
