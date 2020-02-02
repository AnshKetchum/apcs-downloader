/**
 * Assistant.java
 * Function: Command Line Application  that downloads all files necessary 
 * 
 * Compile: javac -cp 'jsoup.jar' Assistant.java 
 * Run: java -cp ".:jsoup.jar" Assistant [Program Name]
 * 
 * @author: Ansh Chaurasia
 * @date 2 / 2 / 2020
 */

//IO Connections
import java.io.*;

//Url Connection 
import java.net.MalformedURLException; 
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.SSLException;

import java.net.HttpURLConnection;


//Jsoup Classes - Parsing HTML
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
 
/*
PDFS (Under Construction)
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser; 
*/

/**
 * TODO LIST 
 * TODO: Add PDF Functionality
 * TODO: Fix any SSL / TCP Errors
 * TODO: Add Prediction Functionality
 */

public class Assistant
{

    /** For Text Files */
    PrintWriter pr;

    /** The code will look in this Directory (APCS database??). 
     * Change it if the link to database changes */
    static final String BASE_URL = "https://drootr.com/apcs/content";

    /** DO NOT TOUCH THESE, THESE LINKS WILL CAUSE THE RECURSION TO BE STUCK */
    static String badRoots [] = {"WebLessons"};
    static String needsRemoval [] = {"Back to APCS Main Page"};

    /** These two define the URL to be checked, and the URL to be ouputte to */
    static String outputFolder = "";
    static String inputUrl = "";

    public Assistant() {}
    
    public static void main(String[] args) throws Exception 
    {
        if(args.length == 0)
            throw new IllegalArgumentException("Program name not specified");
        
        String programName = args[0].trim();   
        inputUrl += BASE_URL + "/" + programName + "/";
        outputFolder += programName + "/";
        new Assistant().createAndDownloadFiles(inputUrl,"");

    }

    /**
     * Gets information from Text Files (.txt, .java, any file that has pure text)
     * @param url The location of the page, as a String, in url format
     * @return The content of the page, listed under the "body" tag, with the html parsed through JSoup API
     */
    public String getContentOfTextFile(String url) 
    {
        String body = "";
        try 
        {
            body = Jsoup.parse(Jsoup.connect(url).get().toString()).body().text().trim();
            for(String badStr : needsRemoval)
                body = body.replace(badStr, "");

        }
        catch (IOException ex) 
        {
            System.err.println("Unable to Connect. Please check your spelling of program. Case does matter!\n");
        }
        return body.trim();
    }

    /**
     * Downloads a .txt file, or .html file
     * @param newURL Local Dir URL
     * @param url Web URL   
     * @param body String version of content of page
     */
    public void downloadWrittenFile(String newURL, String url, String body)
    {
        try
        {
            System.out.println("Creating new Text (Java / HTML) file at URL: " + newURL);
            PrintWriter pr = new PrintWriter(new File(newURL));
            pr.println(body);
            pr.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    
    /** 
     * Downloads an image.
     * @implNote I modified an existing method, found at link: Not My Own Code, taken from: https://www.programcreek.com/2012/12/download-image-from-url-in-java/
     * @param imageUrl Web URL 
     * @param newURL Local Dir URL
     * @throws IOException In case one of the streams fail
     * @throws MalformedURLException In case URL doesn't work.
     */
    public void downloadImage(String imageUrl, String newURL) throws IOException, MalformedURLException
    {
        
        URL url = new URL(imageUrl);
        String fileName = url.getFile();
        String destName = newURL;
     
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(destName);
     
        byte[] b = new byte[2048];
        int length;
     
        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }
     
        is.close();
        os.close();
    }

    /**
     * Downloads a single file. It can handle everything, but the download of PDF Files. 
     * This feature will come in a later update.
     * @param url Web Url of File
     * @param cur File to download
     * @throws IOException In case, the File creation fails.
     */
    public void downloadFile(String url, String cur)  throws IOException
    {
        String newURL = url.replace(inputUrl, outputFolder);
        if(cur.contains(".pdf"))
            return;
        else if(!cur.contains("."))
        {
            File newDir = new File(newURL);
            if(!newDir.exists())
                newDir.mkdir();
        }
        else
        {
            String extension = cur.substring(cur.indexOf(".") + 1);
            switch(extension)
            {
                case "txt":
                    downloadWrittenFile(newURL,url, getContentOfTextFile(url));
                    break;
                case "html":
                    String content = getContentOfTextFile(url);
                    if(content.contains("public class "))
                        downloadWrittenFile(newURL.replace("html", "java"),url, content);
                    else if(content.trim().length() == 0)
                        break;
                    else
                        downloadWrittenFile(newURL.replace("html", "txt"),url, content);                
                    break;
                default:
                    downloadImage(url,newURL);
                    break; 
            }                

        }

    }

    /**
     * Launches a Recursive 'Depth First Search' on the starting point (Mr DeRuiter's website).
     * It continues down until it reaches a file with a '.' extension. 
     * While doing so, it locally downloads the file in the current directory,
     * @param url The web url
     * @param curFile Current File Name
     * @throws MalformedURLException Standard Exception, mainly due to bad url. 
     */
    public void createAndDownloadFiles(String url, String curFile) throws IOException
    {
        downloadFile(url,curFile);
        if(curFile.contains("."))
            return;
        for(String root : badRoots)
            if(root.equalsIgnoreCase(curFile))
                return;

        Document doc;
        try
        {
            doc = Jsoup.connect(url).get();   
        }
        catch(MalformedURLException | SSLException e)
        {
            System.out.println("Error: Unable to Visit URL: " + url);
            return;
        }

        for(Element child : doc.select("a"))
        {
            String name = child.text();
            if(name.contains("Name") || name.equalsIgnoreCase("Last Modified") || name.contains("Size") ||   name.contains("Description") || name.contains("Parent Directory"))
                continue;    
            createAndDownloadFiles(child.attr("abs:href"), name);
        }
    }
    
}