import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import java.io.IOException;
import java.net.URL;

public class Medimsight {

    // Medimsight Service Account Client ID. Replace with your account.
    static String CLIENT_ACCOUNT = "";

    // Private key from the private_key field in the download PEM file.
    static String PRIVATE_KEY = "";

    // Private key path.
    static String PRIVATE_KEY_PATH = "";

    // base url of Medimsight cloud storage objects
    static final String BASE_MEDIMSIGHT_URL = "https://prdmedimsight.appspot.com";

    // expiry time of the url in Linux epoch form (seconds since january 1970)
    static String expiryTime;

    private static final String USER_AGENT = "Mozilla/5.0";

    public void config (String newKey, String newID) {
        PRIVATE_KEY_PATH = newKey;
        CLIENT_ACCOUNT = newID;

        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(newKey));
        } catch (IOException e) {
            e.printStackTrace();
        }

        PRIVATE_KEY = new String(encoded, StandardCharsets.UTF_8);
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

    // Use SHA256withRSA to sign the request
    private static String signString(String input) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(getPrivateKey());
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

    public String getCall(String resource, String[] args) throws IOException {
        String FULL_OBJECT_URL = BASE_MEDIMSIGHT_URL + "/" + resource;

        // Set Url expiry to one minute from now!
        setExpiryTimeInEpoch();

        String stringToSign = "GET" + "\n"
                + "" + "\n"
                + "" + "\n"
                + expiryTime + "\n"
                + "/" + resource;

        String signedString = null;
        try {
            signedString = signString(stringToSign);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // URL encode the signed string so that we can add this URL
        try {
            signedString = URLEncoder.encode(signedString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String signedUrl = FULL_OBJECT_URL
                + "?AccessId=" + CLIENT_ACCOUNT
                + "&Expires=" + expiryTime
                + "&Signature=" + signedString;

        for (String myStr : args) {
            signedUrl += "&" + myStr;
        }

        URL obj = null;
        try {
            obj = new URL(signedUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();

        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            return response.toString();
        } else {
            return "Error: GET request not worked";
        }
    }

    public String postCall(String resource, String[] args) throws IOException {
        String FULL_OBJECT_URL = BASE_MEDIMSIGHT_URL + "/" + resource;

        // Set Url expiry to one minute from now!
        setExpiryTimeInEpoch();

        String stringToSign = "POST" + "\n"
                + "" + "\n"
                + "" + "\n"
                + expiryTime + "\n"
                + "/" + resource;

        String signedString = null;
        try {
            signedString = signString(stringToSign);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // URL encode the signed string so that we can add this URL
        try {
            signedString = URLEncoder.encode(signedString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String signedUrl = FULL_OBJECT_URL;

        URL obj = null;
        try {
            obj = new URL(signedUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        StringBuilder postData = new StringBuilder();
        for (String myStr : args) {
            if (postData.length() != 0) postData.append("&");
            postData.append(myStr);
        }
        postData.append("&_method=post");
        postData.append("&AccessId=" + CLIENT_ACCOUNT);
        postData.append("&Expires=" + expiryTime);
        postData.append("&Signature=" + signedString);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // For POST only - START
        // Send post request
        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));

        writer.write(postData.toString());
        writer.flush();
        writer.close();
        os.close();

       /* DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes( postData.toString() );
        wr.flush();
        wr.close();*/
        // For POST only - END

        int responseCode = con.getResponseCode();

        System.out.println("POST Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            return response.toString();
        } else {
            return "Error: POST request not worked";
        }
    }
}