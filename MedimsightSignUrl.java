import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

// A stand-alone program to generate Medimsight cloud storage signed url for a file.
public class MedimsightSignUrl {

    // Medimsight Service Account Client ID. Replace with your account.
    static final String CLIENT_ACCOUNT = "u@email.com";

    // Private key from the private_key field in the download JSON file. Replace
    // this!
    static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY -----\nMIIEvgIBADANBgk sd\n-----END PRIVATE KEY-----\n";

    // base url of Medimsight cloud storage objects
    static final String BASE_MEDIMSIGHT_URL = "https://prdmedimsight.appspot.com";

    // object we are trying to access. Replace this!
    static final String OBJECT_PATH = "/subject";

    // full url to the object.
    static final String FULL_OBJECT_URL = BASE_MEDIMSIGHT_URL + OBJECT_PATH;

    // expiry time of the url in Linux epoch form (seconds since january 1970)
    static String expiryTime;

    public static void main(String[] args) throws Exception {

        // Set Url expiry to one minute from now!
        setExpiryTimeInEpoch();

        String stringToSign = getSignInput();
        PrivateKey pk = getPrivateKey();
        String signedString = getSignedString(stringToSign, pk);

        // URL encode the signed string so that we can add this URL
        signedString = URLEncoder.encode(signedString, "UTF-8");

        String signedUrl = getSignedUrl(signedString);
        System.out.println(signedUrl);
    }

    // Set an expiry date for the signed url. Sets it at one minute ahead of
    // current time.
    // Represented as the epoch time (seconds since 1st January 1970)
    private static void setExpiryTimeInEpoch() {
        long now = System.currentTimeMillis();
        // expire in a minute!
        // note the conversion to seconds as needed by GCS.
        long expiredTimeInSeconds = (now + 60 * 1000L) / 1000;
        expiryTime = expiredTimeInSeconds + "";
    }

    // The signed URL format as required by Medimsight.
    private static String getSignedUrl(String signedString) {
        String signedUrl = FULL_OBJECT_URL 
                           + "?AccessId=" + CLIENT_ACCOUNT 
                           + "&Expires=" + expiryTime
                           + "&Signature=" + signedString;
        return signedUrl;
    }

    // We sign the expiry time and object path
    private static String getSignInput() {
        return "GET" + "\n" 
                    + "" + "\n" 
                    + "" + "\n" 
                    + expiryTime + "\n" 
                    + OBJECT_PATH;
    }

    // Use SHA256withRSA to sign the request
    private static String getSignedString(String input, PrivateKey pk) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(pk);
        privateSignature.update(input.getBytes("UTF-8"));
        byte[] s = privateSignature.sign();
        return Base64.getEncoder().encodeToString(s);
    }

    // Get private key object from unencrypted PKCS#8 file content
    private static PrivateKey getPrivateKey() throws Exception {
        // Remove extra characters in private key.
        String realPK = PRIVATE_KEY.replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN PRIVATE KEY-----", "").replaceAll("\n", "");
        byte[] b1 = Base64.getDecoder().decode(realPK);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b1);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

}