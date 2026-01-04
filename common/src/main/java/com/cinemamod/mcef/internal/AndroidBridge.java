package com.cinemamod.mcef.internal;

import com.cinemamod.mcef.MCEF;
import java.lang.reflect.Method;

public class AndroidBridge {

    private static Object activity;
    private static Object context;

    public static boolean isAndroid() {
        return System.getProperty("os.name").toLowerCase().contains("android");
    }

    public static Object getContext() {
        if (context == null) {
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
                Object activityThread = currentActivityThreadMethod.invoke(null);
                Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
                context = getApplicationMethod.invoke(activityThread);
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to get Android context via reflection", e);
            }
        }
        return context;
    }

    public static Object getActivity() {
        if (activity == null) {
            try {
                // 通用的获取当前 Activity 的方法：遍历 ActivityThread 中的 mActivities
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
                Object activityThread = currentActivityThreadMethod.invoke(null);
                
                java.lang.reflect.Field mActivitiesField = activityThreadClass.getDeclaredField("mActivities");
                mActivitiesField.setAccessible(true);
                Object mActivities = mActivitiesField.get(activityThread);
                
                if (mActivities instanceof java.util.Map) {
                    java.util.Map<?, ?> activities = (java.util.Map<?, ?>) mActivities;
                    for (Object activityRecord : activities.values()) {
                        java.lang.reflect.Field pausedField = activityRecord.getClass().getDeclaredField("paused");
                        pausedField.setAccessible(true);
                        if (!pausedField.getBoolean(activityRecord)) {
                            java.lang.reflect.Field activityField = activityRecord.getClass().getDeclaredField("activity");
                            activityField.setAccessible(true);
                            activity = activityField.get(activityRecord);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                MCEF.getLogger().error("Failed to get Android activity via reflection", e);
            }
        }
        return activity;
    }

    public static void runOnUiThread(Runnable runnable) {
        try {
            Object act = getActivity();
            if (act != null) {
                Method runOnUiThreadMethod = act.getClass().getMethod("runOnUiThread", Runnable.class);
                runOnUiThreadMethod.invoke(act, runnable);
            }
        } catch (Exception e) {
            MCEF.getLogger().error("Failed to run on UI thread", e);
        }
    }
}
