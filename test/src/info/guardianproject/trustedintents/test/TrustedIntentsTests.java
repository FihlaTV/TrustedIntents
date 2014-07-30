
package info.guardianproject.trustedintents.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.text.TextUtils;
import android.util.Log;

import com.android.AndroidIncludedAppsPin;
import com.android.AndroidSystemPin;

import info.guardianproject.trustedintents.ApkSignaturePin;
import info.guardianproject.trustedintents.TrustedIntents;

import java.util.ArrayList;

public class TrustedIntentsTests extends AndroidTestCase {
    private static final String TAG = "TrustedIntentsTests";

    Context context;
    PackageManager pm;
    TrustedIntents ti;
    String[] packagesSignedByAndroidIncludedApps;
    String[] packagesSignedByAndroidSystem;

    @Override
    public void setUp() {
        context = getContext();
        pm = context.getPackageManager();
        ti = TrustedIntents.get(context);
        ti.removeAllTrustedSigners();

        String[] androidIncludedApps = {
                "com.android.browser", "com.android.calculator2", "com.android.calendar",
                "com.android.dreams.basic", "com.android.providers.calendar",
                "com.android.camera", "com.android.deskclock", "com.android.gesture.builder",
                "com.android.smoketest", "com.android.smoketest.tests",
                "com.android.emulator.connectivity.test", "com.android.development_settings",
                "com.android.email", "com.example.android.livecubes", "com.android.exchange"
        };
        ArrayList<String> androidIncludedAppsInstalled = new ArrayList<String>(
                androidIncludedApps.length);
        for (String packageName : androidIncludedApps) {
            try {
                pm.getPackageInfo(packageName, 0);
                androidIncludedAppsInstalled.add(packageName);
            } catch (NameNotFoundException e) {
                Log.w(TAG, packageName + " not installed on this device");
            }
        }
        packagesSignedByAndroidIncludedApps = androidIncludedAppsInstalled.toArray(new String[0]);

        String[] androidSystem = {
                "android", "com.android.certinstaller", "com.android.backupconfirm",
                "com.android.keyguard", "com.android.sdksetup", "com.android.sharedstoragebackup",
                "com.android.customlocale2", "com.android.development", "com.android.documentsui",
                "com.android.externalstorage", "com.android.location.fused",
                "com.android.inputdevices"
        };
        ArrayList<String> androidSystemInstalled = new ArrayList<String>(
                androidSystem.length);
        for (String packageName : androidSystem) {
            try {
                pm.getPackageInfo(packageName, 0);
                androidSystemInstalled.add(packageName);
            } catch (NameNotFoundException e) {
                Log.w(TAG, packageName + " not installed on this device");
            }
        }
        packagesSignedByAndroidSystem = androidSystemInstalled.toArray(new String[0]);
    }

    private void checkAreSignaturesEqual(String[] packages) {
        Signature[] first = null;
        Signature[] second = null;
        for (int i = 0; i < packages.length; i++) {
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packages[i], PackageManager.GET_SIGNATURES);
                first = pkgInfo.signatures;
            } catch (NameNotFoundException e) {
                Log.w(TAG, "NameNotFoundException on " + packages[i] + ": " + e.getMessage());
                continue;
            }
            for (int j = 0; j < packages.length; j++) {
                if (i == j)
                    continue;
                try {
                    PackageInfo pkgInfo = pm.getPackageInfo(packages[j],
                            PackageManager.GET_SIGNATURES);
                    second = pkgInfo.signatures;
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "NameNotFoundException: " + e.getMessage());
                    continue;
                }
                assertTrue(TrustedIntents.get(context).areSignaturesEqual(first, second));
            }
        }
    }

    /**
     * Intent stores this info internally using {@link CompenentName}, so we're
     * using that as the method for setting it. It can also be set using
     * {@link Intent#setClassName(String, String) setClassName()}, and it is
     * then translated to a {@link ComponentName}.
     *
     * @param packageName
     * @return
     */
    private Intent getLauncherIntent(String packageName) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setPackage(packageName);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        ResolveInfo resolveInfo = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
        if (TextUtils.isEmpty(packageName) || resolveInfo == null)
            return i;
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        assertEquals(activityInfo.packageName, packageName);
        return new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(packageName, activityInfo.name));
    }

    public void testCheckAreSignaturesEqual() {
        checkAreSignaturesEqual(packagesSignedByAndroidIncludedApps);
        checkAreSignaturesEqual(packagesSignedByAndroidSystem);
    }

    public void testCheckAreSignaturesNotEqual() {
        assertFalse(TrustedIntents.get(context).areSignaturesEqual(
                new AndroidIncludedAppsPin().getSignatures(),
                new AndroidSystemPin().getSignatures()));
        PackageInfo pkgInfo;
        Signature[] first = null;
        Signature[] second = null;
        int length = packagesSignedByAndroidSystem.length;
        if (length > packagesSignedByAndroidIncludedApps.length)
            length = packagesSignedByAndroidIncludedApps.length;
        for (int i = 0; i < length; i++) {
            try {
                pkgInfo = pm.getPackageInfo(
                        packagesSignedByAndroidSystem[i],
                        PackageManager.GET_SIGNATURES);
                first = pkgInfo.signatures;
                pkgInfo = pm.getPackageInfo(
                        packagesSignedByAndroidIncludedApps[i],
                        PackageManager.GET_SIGNATURES);
                second = pkgInfo.signatures;
            } catch (NameNotFoundException e) {
                Log.w(TAG, "NameNotFoundException: " + e.getMessage());
                continue;
            }
            assertFalse(TrustedIntents.get(context).areSignaturesEqual(first, second));
        }
    }

    public void testSetPinWithAbstractClass() {
        try {
            ti.addTrustedSigner(ApkSignaturePin.class);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
        }
        assert false;
    }

    public void testIsTrustedSigner() {
        assertFalse(ti.isTrustedSigner(AndroidSystemPin.class));
        assertFalse(ti.isTrustedSigner(AndroidIncludedAppsPin.class));
        /* add one pin as trusted */
        assertTrue(ti.addTrustedSigner(AndroidSystemPin.class));
        assertTrue(ti.isTrustedSigner(AndroidSystemPin.class));
        assertFalse(ti.isTrustedSigner(AndroidIncludedAppsPin.class));
        /* add the other pin as trusted */
        assertTrue(ti.addTrustedSigner(AndroidIncludedAppsPin.class));
        assertTrue(ti.isTrustedSigner(AndroidSystemPin.class));
        assertTrue(ti.isTrustedSigner(AndroidIncludedAppsPin.class));
    }

    public void testCheckPin() {
        Intent intent;

        assertFalse(ti.isReceiverTrusted(new Intent()));
        intent = new Intent();
        intent.setPackage("");
        assertFalse(ti.isReceiverTrusted(intent));
        assertFalse(ti.isReceiverTrusted(getLauncherIntent("")));

        for (String packageName : packagesSignedByAndroidSystem) {
            assertFalse(ti.isReceiverTrusted(getLauncherIntent(packageName)));
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }

        /* packages signed by AndroidSystem should be trusted, others not */
        assertTrue(ti.addTrustedSigner(AndroidSystemPin.class));
        for (String packageName : packagesSignedByAndroidSystem) {
            assertTrue(ti.isReceiverTrusted(getLauncherIntent(packageName)));
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            assertFalse(ti.isReceiverTrusted(getLauncherIntent(packageName)));
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }

        /* all should be trusted now */
        assertTrue(ti.addTrustedSigner(AndroidIncludedAppsPin.class));
        for (String packageName : packagesSignedByAndroidSystem) {
            assertTrue(ti.isReceiverTrusted(getLauncherIntent(packageName)));
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            assertTrue(ti.isReceiverTrusted(getLauncherIntent(packageName)));
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
    }

    public void testRemovePin() {
        Intent intent;

        assertFalse(ti.isReceiverTrusted(new Intent()));
        intent = new Intent();
        intent.setPackage("");
        assertFalse(ti.isReceiverTrusted(intent));

        /* trust everything first */
        assertTrue(ti.addTrustedSigner(AndroidSystemPin.class));
        assertTrue(ti.addTrustedSigner(AndroidIncludedAppsPin.class));
        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }

        /* remove one key, now not everything should be trusted */
        assertTrue(ti.removeTrustedSigner(AndroidIncludedAppsPin.class));
        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }

        /* remove the second pin, nothing should be trusted */
        assertTrue(ti.removeTrustedSigner(AndroidSystemPin.class));
        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }
    }
}
