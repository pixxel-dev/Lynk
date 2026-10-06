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
        if (fullUrl.endsWith("/") && urlPath.startsWith("/")) {
            fullUrl += urlPath.substring(1);
        } else {
            fullUrl += urlPath;
        }

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PROPFIND");
        conn.setRequestProperty("Depth", "1");
        
        String auth = connection.getUsername() + ":" + connection.getPasswordToken();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        
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
                
                // Skip the root folder itself if it matches the requested path exactly
                // Href could be absolute or relative path, so we check carefully
                
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
                        
                        String itemName = decodedHref;
                        if (itemName.endsWith("/")) {
                            itemName = itemName.substring(0, itemName.length() - 1);
                        }
                        int lastSlash = itemName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            itemName = itemName.substring(lastSlash + 1);
                        }
                        
                        // construct webdav:// path
                        String itemPath = "webdav://" + decodedHref;
                        if (!decodedHref.startsWith("/")) {
                            itemPath = "webdav://" + connection.getWebDavUrl() + "/" + decodedHref;
                        } else {
                            // decodedHref is e.g. /disk/folder/
                            // we probably want to maintain the host? 
                            // Actually, let's just make it webdav:/ + decodedHref
                            itemPath = "webdav:/" + decodedHref;
                        }
                        
                        // Skip if it's the requested folder
                        if (itemPath.equals(path) || (itemPath.equals(path + "/")) || (itemPath + "/").equals(path)) {
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
