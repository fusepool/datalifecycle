package eu.fusepool.datalifecycle.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
/**
 * Provides an utility method to retrieve files paths from a local file system or from
 * a remote http server. A file name must end with one of the following extensions: .xml, .rdf.
 * An url that does not end with the mentioned extensions is supposed to refer to a folder in a local file 
 * system (file scheme) or in a remote one (http scheme). 
 * @author luigi
 *
 */

public class FileUtil {
    
    public static ArrayList<String> getFileList(URL url, String [] fileNameExtensions) throws IOException {
        ArrayList<String> fileList = new ArrayList<String>();
        
        String scheme = url.getProtocol();
        String fileName = url.getFile();
        String path = url.getPath();
        String ref = url.toString();
        
        boolean isRdfFile = false;
        for(int i = 0; i < fileNameExtensions.length; i++){
            if(ref.endsWith(fileNameExtensions[i]))
                isRdfFile = true;
        }
        
       
        if(isRdfFile){
            fileList.add(ref);
        }
        else {
            if("file".equals(scheme)) {
                File dir = new File(fileName);
                if(dir.isDirectory()) {
                    if(! path.endsWith("/")) path = path + "/";
                    String [] files = dir.list();
                    for(int i = 0; i < files.length; i++ ) {
                        for(int j = 0; j < fileNameExtensions.length; j++)
                            if(files[i].endsWith(fileNameExtensions[j]))
                                fileList.add(scheme + "://" + path + files[i]);
                    }
                        
                 }
            }
            if("http".equals(scheme)){
                    String html = IOUtils.toString(url);
                    Pattern pattern = Pattern.compile("(<a href=\")(.*?)(\">)");
                    Matcher matcher = pattern.matcher(html);
                    while(matcher.find()){
                        String match = matcher.group(2);
                        for(int i = 0; i < fileNameExtensions.length; i++)                    
                            if(match.endsWith(fileNameExtensions[i]))
                                fileList.add(ref + match);
                    }
            }
        }
        
        return fileList;
    }
    
    public static void main(String [] args) throws IOException {
        //String dataurl = "file:///home/luigi/projects/bfh/fusepool/data_sources/patents/MAREC/rdf/00/";
        //String dataurl = "http://raw.fusepool.info/marec/00/";
        String dataurl = "http://raw.fusepool.info/pmc/Acc_Chem_Res/";
        String [] filenameExtension = {".nxml"};
        URL url = new URL(dataurl);
        ArrayList<String> fileList = FileUtil.getFileList(url, filenameExtension);
        Iterator<String> ifile = fileList.iterator();
        while(ifile.hasNext()){
            System.out.println(ifile.next());
        }
    }

}
