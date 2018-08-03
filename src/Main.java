import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class Main {

    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Usage: 'medimsight <keypath> <cliendID> <filename>'");
            System.exit(1);
        }

        Medimsight mds = new Medimsight();

        mds.config(args[0], args[1]);

        String[] attr = new String[1];
        attr[0] = "wun=all";

        try {
            System.out.println("Finished sending file: " + mds.getCall("group",  attr));
            File fileToUpload = new File(args[2]);

            attr = new String[5];
            attr[0] = "namefile=" + fileToUpload.getName();
            attr[1] = "subject=";
            attr[2] = "class=ZipFile";
            attr[3] = "size=159951";
            attr[4] = "ftype=.zip";
            String signedUrl = mds.postCall("file_0",  attr);

            if ((signedUrl == "request_signature_not_match") || (signedUrl == "request_access_forbidden")) {
                System.out.println("Finished sending file: " + signedUrl);
            }

            String[] newargs = signedUrl.split("\\+\\*\\+");

            attr = new String[6];
            attr[0] = "key="+newargs[1];
            attr[1] = "bucket="+newargs[7];
            attr[2] = "GoogleAccessId="+newargs[2];
            attr[3] = "Expires="+newargs[3];
            attr[4] = "Policy="+newargs[4];
            attr[5] = "signature="+newargs[5];

            // Connect to the web server endpoint
            URL serverUrl =  new URL(newargs[6]);
            HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
            String boundary = Long.toHexString(System.currentTimeMillis());
            // Indicate that we want to write to the HTTP request body
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            StringBuilder postData = new StringBuilder();
            for (String myStr : attr) {
                if (postData.length() != 0) postData.append("&");
                postData.append(myStr);
            }

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

            //writer.write(postData.toString());

            String CRLF = "\r\n";
            // Write the actual file contents
            /*FileInputStream inputStreamToLogFile = new FileInputStream(fileToUpload);

            int bytesRead;
            byte[] dataBuffer = new byte[1024];
            while((bytesRead = inputStreamToLogFile.read(dataBuffer)) != -1) {
                os.write(dataBuffer, 0, bytesRead);
            }
            os.flush();*/
            writer.flush();

            PrintWriter filewriter = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
            filewriter.write(postData.toString());
            // Send binary file.
            filewriter.append("--" + boundary).append(CRLF);
            filewriter.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileToUpload.getName() + "\"").append(CRLF);
            filewriter.append("Content-Type: application/x-zip-compressed").append(CRLF);
            filewriter.append(CRLF).flush();
            Files.copy(fileToUpload.toPath(), os);

            os.flush(); // Important before continuing with writer!
            filewriter.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            filewriter.append("--" + boundary + "--").append(CRLF).flush();

            // Close the streams
            filewriter.close();
            os.close();

            int responseCode = urlConnection.getResponseCode();
            //System.out.println("POST Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // print result
                System.out.println(response.toString());
            } else {
                InputStream stream = urlConnection.getErrorStream();
                if (stream == null) {
                    stream = urlConnection.getInputStream();
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                System.out.println("Error: POST Google request not worked: " + in.readLine());
            }

            attr = new String[2];
            attr[0] = "ack=zip";
            attr[1] = "id="+newargs[0];
            //mds.postCall("file_" + newargs[0],  attr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
