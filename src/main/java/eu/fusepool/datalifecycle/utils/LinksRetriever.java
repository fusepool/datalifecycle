/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.datalifecycle.utils;

import java.io.IOException;
import java.net.MalformedURLException;
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
        //InputStream in = url.openStream();
        String html = IOUtils.toString(url);
        //ByteArrayOutputStream baos 
        //System.out.print(html);
        Pattern pattern = Pattern.compile("(<a href=\")(.*?)(\">)");
        Matcher matcher = pattern.matcher(html);
        List<URL> links = new LinkedList<URL>();
        while (matcher.find()) {
            String linkTarget = matcher.group(2);
            if (linkTarget.endsWith(".rdf") 
                    || linkTarget.endsWith(".nt")
                    || linkTarget.endsWith(".ttl")) {
                links.add(new URL(url,linkTarget));
            }
        }
        return links;
    }
}
