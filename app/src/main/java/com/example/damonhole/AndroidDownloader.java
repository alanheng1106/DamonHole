package com.example.damonhole;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NewPipe Extractor requires a Downloader implementation to make HTTP requests.
 * This bridges it to Android's standard HttpURLConnection.
 */
public class AndroidDownloader extends Downloader {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36";

    @Override
    public Response execute(Request request) throws ReCaptchaException, java.io.IOException {
        String httpMethod  = request.httpMethod();
        String url         = request.url();
        Map<String, List<String>> headers = request.headers();
        byte[] dataToSend  = request.dataToSend();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(httpMethod);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        // Apply headers from the request
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String val : entry.getValue()) {
                    conn.setRequestProperty(entry.getKey(), val);
                }
            }
        }

        // Send body for POST requests
        if (dataToSend != null && dataToSend.length > 0) {
            conn.setDoOutput(true);
            conn.getOutputStream().write(dataToSend);
        }

        int responseCode = conn.getResponseCode();

        // Handle ReCaptcha
        if (responseCode == 429) {
            throw new ReCaptchaException("reCaptcha challenge requested", url);
        }

        // Read response body
        java.io.InputStream inputStream;
        try {
            inputStream = conn.getInputStream();
        } catch (java.io.IOException e) {
            inputStream = conn.getErrorStream();
        }

        String responseBody = "";
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            reader.close();
            responseBody = sb.toString();
        }

        // Collect response headers
        Map<String, List<String>> responseHeaders = new HashMap<>(conn.getHeaderFields());

        return new Response(responseCode, conn.getResponseMessage(), responseHeaders, responseBody, url);
    }
}