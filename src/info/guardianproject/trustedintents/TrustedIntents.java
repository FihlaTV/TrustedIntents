
package info.guardianproject.trustedintents;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.text.TextUtils;

import java.security.cert.CertificateException;
import java.util.LinkedHashSet;

public class TrustedIntents {

    private static TrustedIntents instance;

    private final Context context;
    private PackageManager pm;

    private final LinkedHashSet<ApkSignaturePin> pinList;

    private TrustedIntents(Context context) {
        this.context = context;
        pinList = new LinkedHashSet<ApkSignaturePin>();
    }

    public static TrustedIntents get(Context context) {
        if (instance == null)
            instance = new TrustedIntents(context.getApplicationContext());
        return instance;
    }

    public boolean isReceiverTrusted(Intent intent) {
        if (!isIntentSane(intent))
            return false;
        String packageName = intent.getPackage();
        try {
            checkTrustedSigner(packageName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (CertificateException e) {
            return false;
        }
        return true;
    }

    private boolean isIntentSane(Intent intent) {
        if (intent == null)
            return false;
        String packageName = intent.getPackage();
        if (TextUtils.isEmpty(packageName))
            return false;

        return true;
    }

    /**
     * Add an APK signature that is always trusted for any packageName.
     *
     * @param pin the APK signature to trust
     */
    public void addTrustedSigner(ApkSignaturePin pin) {
        pinList.add(pin);
    }

    public void checkTrustedSigner(String packageName)
            throws NameNotFoundException, CertificateException {
        if (pm == null)
            pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        checkTrustedSigner(packageInfo.signatures);
    }

    public void checkTrustedSigner(PackageInfo packageInfo)
            throws NameNotFoundException, CertificateException {
        checkTrustedSigner(packageInfo.signatures);
    }

    public void checkTrustedSigner(Signature[] signatures)
            throws NameNotFoundException, CertificateException {
        if (signatures == null || signatures.length == 0)
            throw new CertificateException("signatures cannot be null or empty!");
        for (int i = 0; i < signatures.length; i++)
            if (signatures[i] == null || signatures[i].toByteArray().length == 0)
                throw new CertificateException("Certificates cannot be null or empty!");

        // check whether the APK signer is trusted for all apps
        for (ApkSignaturePin pin : pinList)
            if (areSignaturesEqual(signatures, pin.getSignatures()))
                return; // found a matching trusted APK signer

        throw new CertificateException("APK signatures did not match!");
    }

    public boolean areSignaturesEqual(Signature[] sigs0, Signature[] sigs1) {
        // TODO where is Android's implementation of this that I can just call?
        if (sigs0 == null || sigs1 == null)
            return false;
        if (sigs0.length == 0 || sigs1.length == 0)
            return false;
        if (sigs0.length != sigs1.length)
            return false;
        for (int i = 0; i < sigs0.length; i++)
            if (!sigs0[i].equals(sigs1[i]))
                return false;
        return true;
    }

    public void startActivity(Activity activity, Intent intent) throws CertificateException {
        if (!isIntentSane(intent))
            throw new ActivityNotFoundException("The intent was null or empty!");
        String packageName = intent.getPackage();
        try {
            checkTrustedSigner(packageName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            throw new ActivityNotFoundException(e.getLocalizedMessage());
        }
        activity.startActivity(intent);
    }
}
