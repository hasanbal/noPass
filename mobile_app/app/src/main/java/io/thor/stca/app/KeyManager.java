package io.thor.stca.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.KeyGenerator;


public class KeyManager {
    private static final String DEVICE_KEY = "DEVICE_KEY";
    private static final String SEED = "SEED";

    private static KeyManager mInstance;

    public static KeyManager get(Context context) {
        if (mInstance == null) {
            mInstance = new KeyManager();
            mInstance.setContext(context);
        }

        return mInstance;
    }

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    private void setContext(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences("io.thor.stca.app", Context.MODE_PRIVATE);
    }

    public String getDeviceKey() {
        String key = mSharedPreferences.getString(DEVICE_KEY, "7c16d015074026d2d05233aa"); // must be "";

        if (key.length() == 0) {
            key =  UUID.randomUUID().toString().substring(0, 8)
                    + Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);

            mSharedPreferences.edit().putString(DEVICE_KEY, key).apply();
        }

        return key;
    }

    public void setSeed(String seed) {
        mSharedPreferences.edit().putString(SEED, seed).apply();
    }

    public String getSeed() {
        return mSharedPreferences.getString(SEED, "ORSXG5A="); // must be "";
    }

    public String getOneTimeKey() {
        try {
            return Integer.toString(TimeBasedOneTimePasswordUtil.generateCurrentNumber(getSeed()));
        }
        catch (Exception e) {
            e.printStackTrace();

            return "";
        }
    }
}
