package com.example.lynk.core.domain.cloud;

import com.example.lynk.core.domain.file.FileItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class WebDavClient implements CloudStorageClient {

    private final CloudConnection connection;

    public WebDavClient(CloudConnection connection) {
        this.connection = connection;
    }

    public static boolean isYandexPublicLink(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT).trim();
        return u.contains("disk.yandex.ru/d/") || u.contains("yadi.sk/d/")
            || u.contains("disk.yandex.ru/i/") || u.contains("yadi.sk/i/")
            || u.contains("disk.yandex.com/d/") || u.contains("disk.yandex.ru/public/")
            || u.contains("cloud-api.yandex.net");
    }

    public static boolean isGoogleDrivePublicLink(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT).trim();
        return u.contains("drive.google.com") || u.contains("docs.google.com");
    }

    @Override
    public List<FileItem> listFiles(String path) throws Exception {
        if (!path.startsWith("webdav://")) {
            throw new IllegalArgumentException("Path must start with webdav://");
        }

        String baseUrl = connection != null ? connection.getWebDavUrl() : "";
        if (baseUrl == null) {
            baseUrl = "";
        }
        baseUrl = baseUrl.trim();

        if (isYandexPublicLink(baseUrl)) {
            return listYandexPublicFiles(baseUrl, path);
        }

        if (isGoogleDrivePublicLink(baseUrl)) {
            return listGoogleDrivePublicFiles(baseUrl, path);
        }

        String urlPath = path.substring(9);
        if (!urlPath.startsWith("/")) {
            urlPath = "/" + urlPath;
        }

        String fullUrl = baseUrl;
        if (fullUrl.endsWith("/") && urlPath.startsWith("/")) {
            fullUrl += urlPath.substring(1);
        } else if (!fullUrl.endsWith("/") && !urlPath.startsWith("/")) {
            fullUrl += "/" + urlPath;
        } else {
            fullUrl += urlPath;
        }

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PROPFIND");
        conn.setRequestProperty("Depth", "1");

        String username = connection != null ? connection.getUsername() : null;
        String passwordToken = connection != null ? connection.getPasswordToken() : null;
        boolean hasUsername = username != null && !username.trim().isEmpty();
        boolean hasPassword = passwordToken != null && !passwordToken.trim().isEmpty();
        if (hasUsername || hasPassword) {
            String u = hasUsername ? username.trim() : "";
            String p = hasPassword ? passwordToken.trim() : "";
            String auth = u + ":" + p;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            InputStream is = conn.getInputStream();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            List<FileItem> items = new ArrayList<>();
            NodeList responseList = doc.getElementsByTagNameNS("*", "response");

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

            for (int i = 0; i < responseList.getLength(); i++) {
                Element responseNode = (Element) responseList.item(i);

                String href = getElementTextByTagNameNS(responseNode, "*", "href");
                if (href == null) continue;

                String decodedHref = java.net.URLDecoder.decode(href, "UTF-8");

                NodeList propstatList = responseNode.getElementsByTagNameNS("*", "propstat");
                if (propstatList.getLength() > 0) {
                    Element propstatNode = (Element) propstatList.item(0);
                    Element propNode = getElementByTagNameNS(propstatNode, "*", "prop");
                    if (propNode != null) {
                        String contentLengthStr = getElementTextByTagNameNS(propNode, "*", "getcontentlength");
                        long size = 0;
                        if (contentLengthStr != null && !contentLengthStr.isEmpty()) {
                            try {
                                size = Long.parseLong(contentLengthStr);
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }

                        String lastModifiedStr = getElementTextByTagNameNS(propNode, "*", "getlastmodified");
                        long lastModified = 0;
                        if (lastModifiedStr != null && !lastModifiedStr.isEmpty()) {
                            try {
                                Date date = format.parse(lastModifiedStr);
                                if (date != null) {
                                    lastModified = date.getTime();
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        }

                        Element resourceTypeNode = getElementByTagNameNS(propNode, "*", "resourcetype");
                        boolean isDirectory = false;
                        if (resourceTypeNode != null) {
                            Element collectionNode = getElementByTagNameNS(resourceTypeNode, "*", "collection");
                            if (collectionNode != null) {
                                isDirectory = true;
                            }
                        }

                        String pathPart = decodedHref;
                        if (pathPart.startsWith("http://") || pathPart.startsWith("https://")) {
                            try {
                                pathPart = new URL(pathPart).getPath();
                            } catch (Exception ignored) { }
                        }

                        String itemName = pathPart;
                        if (itemName.endsWith("/")) {
                            itemName = itemName.substring(0, itemName.length() - 1);
                        }
                        int lastSlash = itemName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            itemName = itemName.substring(lastSlash + 1);
                        }

                        if (!pathPart.startsWith("/")) {
                            pathPart = "/" + pathPart;
                        }

                        String itemPath = "webdav://" + pathPart;

                        String normItemPath = itemPath.endsWith("/") && itemPath.length() > 10 ? itemPath.substring(0, itemPath.length() - 1) : itemPath;
                        String normReqPath = path.endsWith("/") && path.length() > 10 ? path.substring(0, path.length() - 1) : path;

                        if (normItemPath.equalsIgnoreCase(normReqPath) || itemName.isEmpty()) {
                            continue;
                        }

                        items.add(new FileItem(itemName, itemPath, size, lastModified, isDirectory, true));
                    }
                }
            }
            conn.disconnect();
            return items;
        } else {
            conn.disconnect();
            throw new Exception("WebDAV request failed with code: " + responseCode);
        }
    }

    @Override
    public void downloadFile(FileItem item, File targetFile) throws Exception {
        if (item == null || targetFile == null) {
            throw new IllegalArgumentException("FileItem and targetFile cannot be null");
        }

        String downloadUrl = item.getDownloadUrl();
        String fullUrl;
        boolean needAuth = false;

        if (downloadUrl != null && (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://"))) {
            fullUrl = downloadUrl;
        } else {
            String baseUrl = connection != null ? connection.getWebDavUrl() : "";
            if (baseUrl == null) {
                baseUrl = "";
            }
            baseUrl = baseUrl.trim();

            String urlPath = item.getPath();
            if (urlPath.startsWith("webdav://")) {
                urlPath = urlPath.substring(9);
            }
            if (!urlPath.startsWith("/")) {
                urlPath = "/" + urlPath;
            }

            if (baseUrl.endsWith("/") && urlPath.startsWith("/")) {
                fullUrl = baseUrl + urlPath.substring(1);
            } else if (!baseUrl.endsWith("/") && !urlPath.startsWith("/")) {
                fullUrl = baseUrl + "/" + urlPath;
            } else {
                fullUrl = baseUrl + urlPath;
            }
            needAuth = true;
        }

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        if (needAuth && connection != null) {
            String username = connection.getUsername();
            String passwordToken = connection.getPasswordToken();
            boolean hasUsername = username != null && !username.trim().isEmpty();
            boolean hasPassword = passwordToken != null && !passwordToken.trim().isEmpty();
            if (hasUsername || hasPassword) {
                String u = hasUsername ? username.trim() : "";
                String p = hasPassword ? passwordToken.trim() : "";
                String auth = u + ":" + p;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }
        }

        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307 || responseCode == 308) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                if (needAuth && connection != null) {
                    String username = connection.getUsername();
                    String passwordToken = connection.getPasswordToken();
                    boolean hasUsername = username != null && !username.trim().isEmpty();
                    boolean hasPassword = passwordToken != null && !passwordToken.trim().isEmpty();
                    if (hasUsername || hasPassword) {
                        String u = hasUsername ? username.trim() : "";
                        String p = hasPassword ? passwordToken.trim() : "";
                        String auth = u + ":" + p;
                        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
                    }
                }
                responseCode = conn.getResponseCode();
            }
        }

        if (responseCode >= 200 && responseCode < 300) {
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (InputStream in = conn.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                conn.disconnect();
            }
        } else {
            conn.disconnect();
            throw new Exception("HTTP " + responseCode);
        }
    }

    @Override
    public boolean uploadFile(String folderPath, String fileName, InputStream fileStream) throws Exception {
        if (!folderPath.startsWith("webdav://")) {
            throw new IllegalArgumentException("Path must start with webdav://");
        }

        String baseUrl = connection != null ? connection.getWebDavUrl() : "";
        if (baseUrl == null) {
            baseUrl = "";
        }
        baseUrl = baseUrl.trim();

        if (isYandexPublicLink(baseUrl) || isGoogleDrivePublicLink(baseUrl)) {
            throw new UnsupportedOperationException("Upload is not supported for public links");
        }

        String urlPath = folderPath.substring(9);
        if (!urlPath.startsWith("/")) {
            urlPath = "/" + urlPath;
        }
        if (!urlPath.endsWith("/")) {
            urlPath = urlPath + "/";
        }

        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        urlPath += encodedFileName;

        String fullUrl = baseUrl;
        if (fullUrl.endsWith("/") && urlPath.startsWith("/")) {
            fullUrl += urlPath.substring(1);
        } else if (!fullUrl.endsWith("/") && !urlPath.startsWith("/")) {
            fullUrl += "/" + urlPath;
        } else {
            fullUrl += urlPath;
        }

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);

        String username = connection != null ? connection.getUsername() : null;
        String passwordToken = connection != null ? connection.getPasswordToken() : null;
        boolean hasUsername = username != null && !username.trim().isEmpty();
        boolean hasPassword = passwordToken != null && !passwordToken.trim().isEmpty();
        if (hasUsername || hasPassword) {
            String u = hasUsername ? username.trim() : "";
            String p = hasPassword ? passwordToken.trim() : "";
            String auth = u + ":" + p;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            if (fileStream != null) {
                fileStream.close();
            }
        }

        int responseCode = conn.getResponseCode();
        return responseCode >= 200 && responseCode < 300;
    }

    @SuppressWarnings("unchecked")
    private List<FileItem> listYandexPublicFiles(String publicKeyUrl, String path) throws Exception {
        String relativePath = "";
        if (path != null && path.startsWith("webdav://")) {
            relativePath = path.substring(9);
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        StringBuilder apiUrlBuilder = new StringBuilder("https://cloud-api.yandex.net/v1/disk/public/resources");
        apiUrlBuilder.append("?public_key=").append(java.net.URLEncoder.encode(publicKeyUrl, "UTF-8"));
        if (!relativePath.isEmpty()) {
            apiUrlBuilder.append("&path=").append(java.net.URLEncoder.encode("/" + relativePath, "UTF-8"));
        }
        apiUrlBuilder.append("&limit=1000");

        URL url = new URL(apiUrlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            InputStream is = conn.getInputStream();
            java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String jsonStr = result.toString("UTF-8");
            conn.disconnect();

            List<FileItem> items = new ArrayList<>();
            Object parsed = new JsonParser(jsonStr).parse();
            if (parsed instanceof Map) {
                Map<String, Object> root = (Map<String, Object>) parsed;
                Object embeddedObj = root.get("_embedded");
                if (embeddedObj instanceof Map) {
                    Map<String, Object> embedded = (Map<String, Object>) embeddedObj;
                    Object itemsObj = embedded.get("items");
                    if (itemsObj instanceof List) {
                        List<Object> itemsList = (List<Object>) itemsObj;
                        for (Object itemElement : itemsList) {
                            if (itemElement instanceof Map) {
                                Map<String, Object> itemObj = (Map<String, Object>) itemElement;
                                String name = getString(itemObj, "name", "Unnamed");
                                String type = getString(itemObj, "type", "");
                                boolean isDir = "dir".equalsIgnoreCase(type);
                                long size = getLong(itemObj, "size", 0L);
                                String modifiedStr = getString(itemObj, "modified", "");
                                long lastModified = parseIsoDate(modifiedStr);
                                String downloadUrl = getString(itemObj, "file", null);
                                String itemPathInResource = getString(itemObj, "path", "");

                                String itemPath = "webdav://" + (itemPathInResource.startsWith("/") ? itemPathInResource : "/" + itemPathInResource);
                                FileItem item = new FileItem(name, itemPath, size, lastModified, isDir, true, downloadUrl);
                                items.add(item);
                            }
                        }
                    }
                } else if ("file".equalsIgnoreCase(getString(root, "type", ""))) {
                    String name = getString(root, "name", "Public File");
                    long size = getLong(root, "size", 0L);
                    long lastModified = parseIsoDate(getString(root, "modified", ""));
                    String downloadUrl = getString(root, "file", null);
                    String itemPath = "webdav:///" + name;
                    items.add(new FileItem(name, itemPath, size, lastModified, false, true, downloadUrl));
                }
            }

            return items;
        } else {
            conn.disconnect();
            throw new Exception("Yandex Public Disk API failed with code: " + responseCode);
        }
    }

    private List<FileItem> listGoogleDrivePublicFiles(String urlStr, String path) {
        List<FileItem> items = new ArrayList<>();
        String fileId = null;
        if (urlStr.contains("/d/")) {
            int idx = urlStr.indexOf("/d/");
            String after = urlStr.substring(idx + 3);
            int slash = after.indexOf('/');
            if (slash >= 0) {
                fileId = after.substring(0, slash);
            } else {
                fileId = after;
            }
        } else if (urlStr.contains("id=")) {
            int idx = urlStr.indexOf("id=");
            String after = urlStr.substring(idx + 3);
            int amp = after.indexOf('&');
            if (amp >= 0) {
                fileId = after.substring(0, amp);
            } else {
                fileId = after;
            }
        }

        if (fileId != null && !fileId.isEmpty()) {
            String directUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
            items.add(new FileItem("GoogleDriveFile_" + fileId, "webdav:///GoogleDriveFile", 0L, System.currentTimeMillis(), false, true, directUrl));
        }
        return items;
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : defaultVal;
    }

    private long getLong(Map<String, Object> map, String key, long defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultVal;
    }

    private long parseIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0L;
        try {
            String cleaned = dateStr.replaceAll("Z$", "+0000").replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            Date date = sdf.parse(cleaned);
            return date != null ? date.getTime() : 0L;
        } catch (Exception e) {
            try {
                String cleaned = dateStr.replaceAll("Z$", "+0000").replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
                Date date = sdf2.parse(cleaned);
                return date != null ? date.getTime() : 0L;
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }

    private Element getElementByTagNameNS(Element parent, String namespaceURI, String localName) {
        NodeList nodeList = parent.getElementsByTagNameNS(namespaceURI, localName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }
        return null;
    }

    private String getElementTextByTagNameNS(Element parent, String namespaceURI, String localName) {
        Element element = getElementByTagNameNS(parent, namespaceURI, localName);
        if (element != null) {
            return element.getTextContent();
        }
        return null;
    }

    public static class JsonParser {
        private final String json;
        private int pos = 0;

        public JsonParser(String json) {
            this.json = json != null ? json : "";
        }

        public Object parse() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (Character.isDigit(c) || c == '-') return parseNumber();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return null;
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < json.length()) {
                skipWhitespace();
                if (json.charAt(pos) != '"') break;
                String key = parseString();
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ':') pos++;
                skipWhitespace();
                Object value = parse();
                map.put(key, value);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else if (pos < json.length() && json.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < json.length()) {
                skipWhitespace();
                Object val = parse();
                list.add(val);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else if (pos < json.length() && json.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            pos++; // skip opening '"'
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < json.length()) {
                    char next = json.charAt(pos++);
                    if (next == 'n') sb.append('\n');
                    else if (next == 'r') sb.append('\r');
                    else if (next == 't') sb.append('\t');
                    else if (next == 'u' && pos + 4 <= json.length()) {
                        String hex = json.substring(pos, pos + 4);
                        pos += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ignored) { }
                    } else sb.append(next);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (json.charAt(pos) == '-') pos++;
            while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = json.substring(start, pos);
            if (numStr.contains(".")) {
                try { return Double.parseDouble(numStr); } catch (Exception e) { return 0; }
            } else {
                try { return Long.parseLong(numStr); } catch (Exception e) { return 0L; }
            }
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) { pos += 4; return true; }
            if (json.startsWith("false", pos)) { pos += 5; return false; }
            return false;
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) { pos += 4; }
            return null;
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
