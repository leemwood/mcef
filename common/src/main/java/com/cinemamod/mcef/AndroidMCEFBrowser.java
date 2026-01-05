package com.cinemamod.mcef;

import com.cinemamod.mcef.internal.AndroidBridge;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AndroidMCEFBrowser implements IMCEFBrowser {

    private final MCEFRenderer renderer;
    private String url;
    private boolean transparent;
    private int width, height;
    
    private Object webView;
    private ByteBuffer pixelBuffer;
    private int[] pixels;
    private boolean closing = false;

    public AndroidMCEFBrowser(String url, boolean transparent) {
        this.url = url;
        this.transparent = transparent;
        this.renderer = new MCEFRenderer(transparent);
        
        Minecraft.getInstance().submit(renderer::initialize);
        
        AndroidBridge.runOnUiThread(() -> {
            try {
                Object context = AndroidBridge.getContext();
                Class<?> webViewClass = Class.forName("android.webkit.WebView");
                Constructor<?> webViewConstructor = webViewClass.getConstructor(Class.forName("android.content.Context"));
                webView = webViewConstructor.newInstance(context);
                
                // Enable JS
                Method getSettingsMethod = webViewClass.getMethod("getSettings");
                Object settings = getSettingsMethod.invoke(webView);
                Method setJavaScriptEnabledMethod = settings.getClass().getMethod("setJavaScriptEnabled", boolean.class);
                setJavaScriptEnabledMethod.invoke(settings, true);
                
                // Load URL
                Method loadUrlMethod = webViewClass.getMethod("loadUrl", String.class);
                loadUrlMethod.invoke(webView, url);
                
                // Start rendering loop
                startRenderingLoop();
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to initialize Android WebView", e);
            }
        });
    }

    private void startRenderingLoop() {
        AndroidBridge.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (closing || webView == null) return;
                
                try {
                    updateTexture();
                } catch (Exception e) {
                    MCEF.getLogger().error("Error updating WebView texture", e);
                }
                
                // Schedule next frame (approx 30fps for performance)
                AndroidBridge.runOnUiThread(this);
            }
        });
    }

    private void updateTexture() throws Exception {
        if (width <= 0 || height <= 0) return;
        
        int size = width * height;
        if (pixels == null || pixels.length != size) {
            pixels = new int[size];
            pixelBuffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder());
        }
        
        Class<?> bitmapClass = Class.forName("android.graphics.Bitmap");
        Method createBitmapMethod = bitmapClass.getMethod("createBitmap", int.class, int.class, Class.forName("android.graphics.Bitmap$Config"));
        Class<?> configClass = Class.forName("android.graphics.Bitmap$Config");
        Object config = configClass.getField("ARGB_8888").get(null);
        Object bitmap = createBitmapMethod.invoke(null, width, height, config);
        
        Class<?> canvasClass = Class.forName("android.graphics.Canvas");
        Constructor<?> canvasConstructor = canvasClass.getConstructor(bitmapClass);
        Object canvas = canvasConstructor.newInstance(bitmap);
        
        Method drawMethod = webView.getClass().getMethod("draw", canvasClass);
        drawMethod.invoke(webView, canvas);
        
        Method getPixelsMethod = bitmapClass.getMethod("getPixels", int[].class, int.class, int.class, int.class, int.class, int.class, int.class);
        getPixelsMethod.invoke(bitmap, pixels, 0, width, 0, 0, width, height);
        
        // Convert ARGB to RGBA for GLES
        pixelBuffer.clear();
        for (int i = 0; i < size; i++) {
            int p = pixels[i];
            byte a = (byte) ((p >> 24) & 0xFF);
            byte r = (byte) ((p >> 16) & 0xFF);
            byte g = (byte) ((p >> 8) & 0xFF);
            byte b = (byte) (p & 0xFF);
            pixelBuffer.put(r).put(g).put(b).put(a);
        }
        pixelBuffer.flip();
        
        Minecraft.getInstance().submit(() -> {
            renderer.onPaint(pixelBuffer, width, height);
        });
        
        Method recycleMethod = bitmapClass.getMethod("recycle");
        recycleMethod.invoke(bitmap);
    }

    @Override
    public void loadURL(String url) {
        this.url = url;
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method loadUrlMethod = webView.getClass().getMethod("loadUrl", String.class);
                loadUrlMethod.invoke(webView, url);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to load URL", e);
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method layoutMethod = webView.getClass().getMethod("layout", int.class, int.class, int.class, int.class);
                layoutMethod.invoke(webView, 0, 0, width, height);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to resize WebView", e);
            }
        });
    }

    @Override
    public void close() {
        closing = true;
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method destroyMethod = webView.getClass().getMethod("destroy");
                destroyMethod.invoke(webView);
                webView = null;
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to destroy WebView", e);
            }
        });
        renderer.cleanup();
    }

    @Override
    public MCEFRenderer getRenderer() {
        return renderer;
    }

    private void dispatchMotionEvent(int action, float x, float y) {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Class<?> motionEventClass = Class.forName("android.view.MotionEvent");
                Method obtainMethod = motionEventClass.getMethod("obtain", long.class, long.class, int.class, float.class, float.class, int.class);
                
                long now = System.currentTimeMillis();
                Object event = obtainMethod.invoke(null, now, now, action, x, y, 0);
                
                Method dispatchTouchEventMethod = webView.getClass().getMethod("dispatchTouchEvent", motionEventClass);
                dispatchTouchEventMethod.invoke(webView, event);
                
                Method recycleMethod = motionEventClass.getMethod("recycle");
                recycleMethod.invoke(event);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to dispatch MotionEvent", e);
            }
        });
    }

    private void dispatchKeyEvent(int action, int keyCode) {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Class<?> keyEventClass = Class.forName("android.view.KeyEvent");
                Constructor<?> keyEventConstructor = keyEventClass.getConstructor(int.class, int.class);
                Object event = keyEventConstructor.newInstance(action, keyCode);
                
                Method dispatchKeyEventMethod = webView.getClass().getMethod("dispatchKeyEvent", keyEventClass);
                dispatchKeyEventMethod.invoke(webView, event);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to dispatch KeyEvent", e);
            }
        });
    }

    @Override
    public void sendMouseMove(int x, int y, int modifiers) {
        // ACTION_MOVE = 2
        dispatchMotionEvent(2, (float) x, (float) y);
    }

    @Override
    public void sendMousePress(int x, int y, int modifiers, int button, boolean isRelease, int clickCount) {
        // ACTION_DOWN = 0, ACTION_UP = 1
        int action = isRelease ? 1 : 0;
        dispatchMotionEvent(action, (float) x, (float) y);
    }

    @Override
    public void sendMouseWheel(int x, int y, int modifiers, int delta) {
        // WebView doesn't have a simple way to dispatch mouse wheel via dispatchTouchEvent
        // Usually handled via scrollBy or similar. For now, we can skip or implement scrollBy.
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method scrollByMethod = webView.getClass().getMethod("scrollBy", int.class, int.class);
                scrollByMethod.invoke(webView, 0, -delta);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to send mouse wheel", e);
            }
        });
    }

    @Override
    public void sendKeyPress(int key, char character, int modifiers) {
        // ACTION_DOWN = 0
        int androidKeyCode = translateToAndroidKeyCode(key);
        if (androidKeyCode != 0) {
            dispatchKeyEvent(0, androidKeyCode);
        }
    }

    @Override
    public void sendKeyRelease(int key, char character, int modifiers) {
        // ACTION_UP = 1
        int androidKeyCode = translateToAndroidKeyCode(key);
        if (androidKeyCode != 0) {
            dispatchKeyEvent(1, androidKeyCode);
        }
    }

    @Override
    public void sendKeyType(int key, char character, int modifiers) {
        // For text input, we might need a different approach or just rely on sendKeyPress/Release
    }

    private int translateToAndroidKeyCode(int glfwKey) {
        // Simple mapping for common keys. Full mapping would be very large.
        // GLFW key codes are used by Minecraft.
        switch (glfwKey) {
            case 257: return 66; // ENTER -> KEYCODE_ENTER
            case 258: return 61; // TAB -> KEYCODE_TAB
            case 259: return 67; // BACKSPACE -> KEYCODE_DEL
            case 262: return 22; // RIGHT -> KEYCODE_DPAD_RIGHT
            case 263: return 21; // LEFT -> KEYCODE_DPAD_LEFT
            case 264: return 20; // DOWN -> KEYCODE_DPAD_DOWN
            case 265: return 19; // UP -> KEYCODE_DPAD_UP
            case 32:  return 62; // SPACE -> KEYCODE_SPACE
            // Numbers
            case 48: return 7;  // 0
            case 49: return 8;  // 1
            case 50: return 9;  // 2
            case 51: return 10; // 3
            case 52: return 11; // 4
            case 53: return 12; // 5
            case 54: return 13; // 6
            case 55: return 14; // 7
            case 56: return 15; // 8
            case 57: return 16; // 9
            // Letters (A-Z) - Simplified mapping
            default:
                if (glfwKey >= 65 && glfwKey <= 90) {
                    return glfwKey - 65 + 29; // A is 29 in Android
                }
                return 0;
        }
    }

    @Override
    public void runJS(String script, String url) {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method evaluateJavascriptMethod = webView.getClass().getMethod("evaluateJavascript", String.class, Class.forName("android.webkit.ValueCallback"));
                evaluateJavascriptMethod.invoke(webView, script, null);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to evaluate JS", e);
            }
        });
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public boolean canGoForward() {
        return false;
    }

    @Override
    public void goBack() {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method goBackMethod = webView.getClass().getMethod("goBack");
                goBackMethod.invoke(webView);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to go back", e);
            }
        });
    }

    @Override
    public void goForward() {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method goForwardMethod = webView.getClass().getMethod("goForward");
                goForwardMethod.invoke(webView);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to go forward", e);
            }
        });
    }

    @Override
    public void reload() {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method reloadMethod = webView.getClass().getMethod("reload");
                reloadMethod.invoke(webView);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to reload", e);
            }
        });
    }

    @Override
    public void reloadIgnoreCache() {
        reload();
    }

    @Override
    public void stopLoad() {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method stopLoadingMethod = webView.getClass().getMethod("stopLoading");
                stopLoadingMethod.invoke(webView);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to stop loading", e);
            }
        });
    }

    @Override
    public void setFocus(boolean focus) {
        AndroidBridge.runOnUiThread(() -> {
            if (webView == null) return;
            try {
                Method requestFocusMethod = webView.getClass().getMethod("requestFocus");
                requestFocusMethod.invoke(webView);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to set focus", e);
            }
        });
    }

    @Override
    public boolean isTransparent() {
        return transparent;
    }
}
