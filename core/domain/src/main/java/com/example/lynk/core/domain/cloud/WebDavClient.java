package com.example.lynk.core.domain.cloud;

import com.example.lynk.core.domain.file.FileItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class WebDavClient implements CloudStorageClient {

    private final CloudConnection connection;

    public WebDavClient(CloudConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<FileItem> listFiles(String path) throws Exception {
        if (!path.startsWith("webdav://")) {
            throw new IllegalArgumentException("Path must start with webdav://");
        }
        
        String urlPath = path.substring(9);
        if (!urlPath.startsWith("/")) {
            urlPath = "/" + urlPath;
        }
        
        String fullUrl = connection.getWebDavUrl();
        if (fullUrl == null) {
            fullUrl = "";
        }
        fullUrl = fullUrl.trim();
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
}
