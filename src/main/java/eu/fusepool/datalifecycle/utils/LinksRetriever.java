/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.datalifecycle.utils;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Reto
 */
public class LinksRetriever {

    
    public static List<URL> getLinks(String urlString) throws IOException {
        URL url = new URL(urlString);
        return getLinks(url);
    }
    public static List<URL> getLinks(URL url) throws IOException {
        return getLinks(url, false);
    }
    public static List<URL> getLinks(URL url, boolean recurse) throws IOException {
        List<URL> links = new LinkedList<URL>();
        getLinks(url, recurse, links);
        return links;
    }
    
    private static void getLinks(URL url, boolean recurse, List<URL> links) throws IOException {
        //InputStream in = url.openStream();
        String html = IOUtils.toString(url);
        //ByteArrayOutputStream baos 
        //System.out.print(html);
        Pattern pattern = Pattern.compile("(<a href=\")(.*?)(\">)");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String linkTarget = matcher.group(2);
            URL targetUrl = new URL(url,linkTarget);
            if (linkTarget.endsWith(".rdf") 
                    || linkTarget.endsWith(".nt")
                    || linkTarget.endsWith(".ttl")
                    || linkTarget.endsWith(".xml")) {
                links.add(targetUrl);
                continue;
            }
            if (recurse && linkTarget.endsWith("/") 
                    && (targetUrl.toString().startsWith(url.toString()))
                    && (!targetUrl.equals(url))) {
                getLinks(targetUrl, recurse, links);
            }
        }
    }
}
