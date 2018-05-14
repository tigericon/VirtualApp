package com.lody.virtual.helper.compat;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UriCompat {
    private static final String TAG = "UriCompat";
    private static boolean DEBUG = false;
    public static String AUTH = "virtual.fileprovider";

    public static boolean needFake(Intent intent) {
        String pkg = intent.getPackage();
        //inside
        if (pkg != null && VirtualCore.get().isAppInstalled(pkg)) {
            return false;
        }
        //inside
        ComponentName componentName = intent.getComponent();
        if (componentName != null && VirtualCore.get().isAppInstalled(componentName.getPackageName())) {
            return false;
        }
        //fake all intent's uri
        return true;
    }

    public static Uri fakeFileUri(Uri uri) {
        if (uri == null) return null;
        if ("content".equals(uri.getScheme())) {
            //fake auth
            String auth = uri.getAuthority();
            if(AUTH.equals(auth)){
                return uri;
            }
            Uri u = uri.buildUpon().authority(AUTH).appendQueryParameter("__va_auth", auth).build();
            if (DEBUG) {
                Log.i(TAG, "fake uri:" + uri + "->" + u);
            }
            return u;
        } else {
            return null;
        }
    }

    public static Uri wrapperUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        String auth = uri.getQueryParameter("__va_auth");
        if (!TextUtils.isEmpty(auth)) {
            Uri.Builder builder = uri.buildUpon().authority(auth);
            Set<String> names = uri.getQueryParameterNames();
            Map<String, String> querys = null;
            if (names != null && names.size() > 0) {
                querys = new HashMap<>();
                for (String name : names) {
                    querys.put(name, uri.getQueryParameter(name));
                }
            }
            builder.clearQuery();
            for (Map.Entry<String, String> e : querys.entrySet()) {
                if(!"__va_auth".equals(e.getKey())) {
                    builder.appendQueryParameter(e.getKey(), e.getValue());
                }
            }
            Uri u = builder.build();
            if (DEBUG) {
                Log.i(TAG, "wrapperUri uri:" + uri + "->" + u);
            }
            return u;
        }
        return uri;
    }

    public static Intent fakeFileUri(Intent intent) {
        if (!needFake(intent)) {
            if (DEBUG) {
                VLog.i(TAG, "don't need fake intent");
            }
            return intent;
        }
        Uri uri = intent.getData();
        if (uri != null) {
            Uri u = fakeFileUri(uri);
            if (u != null) {
                if (DEBUG) {
                    Log.i(TAG, "fake data uri:" + uri + "->" + u);
                }
                intent.setData(u);
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            ClipData data = intent.getClipData();
            if (data != null) {
                int count = data.getItemCount();
                for (int i = 0; i < count; i++) {
                    ClipData.Item item = data.getItemAt(i);
                    Uri u = fakeFileUri(item.getUri());
                    if (u != null) {
                        Reflect.on(item).set("mUri", u);
                    }
                }
            }
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean changed2 = false;
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                Object val = extras.get(key);
                if (val instanceof Intent) {
                    Intent i = fakeFileUri((Intent) val);
                    if (i != null) {
                        changed2 = true;
                        extras.putParcelable(key, i);
                    }
                } else if (val instanceof Uri) {
                    Uri u = fakeFileUri((Uri) val);
                    if (u != null) {
                        changed2 = true;
                        extras.putParcelable(key, u);
                    }
                }
            }
            if (changed2) {
                intent.putExtras(extras);
            }
        }
        return intent;
    }
}
