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
        final List<URL> links = new LinkedList<URL>();
        processLinks(url, recurse, new LinkProcessor() {

            public boolean process(URL url) {
                links.add(url);
                return true;
            }
            
        });
        return links;
    }
    
    public static void processLinks(URL url, boolean recurse, LinkProcessor processor) throws IOException {
        interruptibleProcessLinks(url, recurse, processor);
    }
    /**
     * @return true if the processing shall continue, false otherwise
     */
    private static boolean interruptibleProcessLinks(URL url, boolean recurse, LinkProcessor processor) throws IOException {
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
                if (processor.process(targetUrl)) {
                    continue;
                } else {
                    return false;
                }
            }
            if (recurse && linkTarget.endsWith("/") 
                    && (targetUrl.toString().startsWith(url.toString()))
                    && (!targetUrl.equals(url))) {
                if (!interruptibleProcessLinks(targetUrl, recurse, processor)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static interface LinkProcessor {

        /**
         * Process an URI  
         * @param url the URI to process
         * @return true if the processing shall continue, false otherwise
         */
        boolean process(URL url);
    }
}
