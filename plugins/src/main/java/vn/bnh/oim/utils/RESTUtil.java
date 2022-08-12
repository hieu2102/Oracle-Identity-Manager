package vn.bnh.oim.utils;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class RESTUtil {
    private static final Logger logger = LoggerFactory.getLogger(RESTUtil.class);
    private static final String FROM_ID = "IDM";
    private static final String FROM_NAME = "IDM";
    private static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyyMMddHH24mmss");

    public static JSONObject buildHeader(String resourceObjName) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        JSONObject headerContent = new JSONObject();
        headerContent.put("MessageId", ID_FORMAT.format(timestamp));
        headerContent.put("FromId", FROM_ID);
        headerContent.put("FromName", FROM_NAME);
        headerContent.put("ToId", resourceObjName);
        headerContent.put("ToName", resourceObjName);
        headerContent.put("DateTime", "");
        headerContent.put("Signature", "");
        logger.debug("{}", headerContent);
        return headerContent;
    }

    public static JSONObject callAPI(
            String urlString,
            String httpMethod,
            String resourceObjName,
            JSONObject payload
    ) throws IOException {
//        build request body
        JSONObject headerContent = buildHeader(resourceObjName);
        JSONObject requestBody = new JSONObject();
        requestBody.put("Header", headerContent);
        requestBody.put("Payload", payload);
        URL url = new URL(urlString);
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod(httpMethod);
        OutputStream os = httpCon.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        osw.write(JSONObject.valueToString(requestBody));
        osw.flush();
        osw.close();
        os.close();
        httpCon.connect();

        // read the input stream and print it
        String result;
        BufferedInputStream bis = new BufferedInputStream(httpCon.getInputStream());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result2 = bis.read();
        while (result2 != -1) {
            buf.write((byte) result2);
            result2 = bis.read();
        }
        result = buf.toString();

        return (JSONObject) JSONObject.stringToValue(result);
    }
}
