
import java.util.*;
import java.io.*;

/**
 * general-purpose utility which reads from some configuration files 
 * and generates a set of HTML files representing an "art gallery".
 * each generated file displays one image along with optional description.
 * "forward" and "back" image icons allow the visitor to flip between
 * images in a gallery "room".
 *
 * @author Melinda Green
 */
public class GalleryBuilder {
    // source of gallery-specific strings common to all art pages
    private static final String GALLERY_PROPS_FILE = "galleryprops.txt";
    // template html file with string placeholders for props, etc.
    private static final String CONTENT_FILE_NAME = "content.html";
    // database file
    private static final String DATABASE_FILE_NAME = "art.csv";
    // maps shorthand "type" labels in art CSV file to directory names
    private static final String TYPES[][] =  { 
        {"tran", "translational"},
        {"circ", "circular"},
        {"hype", "hyperbolic"},
        {"misc", "misc"},
    };
    public static void main(String args[]) {
        String content = null;
        String artdata[][] = null;
        Properties string_map = new Properties();
        try {
            // read the content data
            content = file2string(CONTENT_FILE_NAME);
            // read the gallery properties
            string_map.load(new FileInputStream(GALLERY_PROPS_FILE));
            artdata = loadCSV(new BufferedReader(new FileReader(DATABASE_FILE_NAME)));
        }
        catch(IOException ioe) {
            System.err.println("TylerGallery.main: i/o exception " + ioe);
            System.exit(-1);
        }
        //System.out.println(content);
        for(Enumeration e=string_map.propertyNames(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String val = (String)string_map.get(key);
            content = replaceAll(content, key, val);
        }
        String lastfilename = null;
        String curtype = null;
        for(int i=0; i<artdata.length; i++) {
            String date = artdata[i][0].trim();
            String type = artdata[i][1].trim();
            String author = artdata[i][2].trim();
            String imageFile = artdata[i][3].trim();
            String comment = artdata[i][4].trim(); // currently ignored
            String artcontent = content;
            String back = lastfilename;
            String forward = (String)string_map.get("GALLERY");
            if ( ! type.equalsIgnoreCase(curtype)) { // no previous image of this type
                curtype = type;
                back = (String)string_map.get("GALLERY");
            }
            if(i<artdata.length-1 && type.equalsIgnoreCase(artdata[i+1][1])) { // "next" image exists
                String nextImage = artdata[i+1][3];
                forward = nextImage.substring(0, nextImage.indexOf('.')).trim() + ".html";
            }
            System.out.println("image " + (i+1) + "/" + artdata.length + ": type=" + type + " curtype=" + curtype + " " + imageFile);
            artcontent = replaceAll(artcontent, "PREV", back);
            artcontent = replaceAll(artcontent, "NEXT", forward);
            lastfilename = writeHTML(artcontent, date, type, author, imageFile);
        }
    }
    
    private static String[][] loadCSV(BufferedReader reader) {
        try {
            ArrayList lines = new ArrayList();
            while (true) {
                String line = reader.readLine();
                if(line == null)
                    break;
                if(line.length()==0 || line.charAt(0)=='#')
                    continue;
                StringTokenizer st = new StringTokenizer(line, ",");
                String fields[] = new String[st.countTokens()];
                for(int i=0; i<fields.length; i++)
                    fields[i] = st.nextToken().trim();
                lines.add(fields);    
            }
            return (String[][])lines.toArray(new String[1][5]);
        } catch(IOException ioe) {
            return null;
        }
    }
    
    private static String file2string(String fname) {
        try {
                BufferedReader reader = new BufferedReader(new FileReader(fname));
                StringBuffer contentBuffer = new StringBuffer();
                boolean readOK = true;
                while(true) {
                    String line = reader.readLine();
                    if (line == null) {
                        reader.close();
                        return contentBuffer.toString();
                    }
                    contentBuffer.append(line);    
                    contentBuffer.append('\n');    
                }
        }
        catch(IOException ioe) {
            System.err.println("TylerGallery.main: i/o exception " + ioe);
            System.exit(-1);
        }
        return null;
    }
    
    private static String writeHTML(String content, String creationDate, String type, String author, String imageFile) {
        String fullTypeName = null;
        for(int i=0; i<TYPES.length; i++) {
            String si = TYPES[i][0];
            if(type.equalsIgnoreCase(si))
                fullTypeName = TYPES[i][1];
        }    
        content = replaceAll(content, "TYPE", fullTypeName);
        content = replaceAll(content, "IMAGE_FILE", imageFile);
        content = replaceAll(content, "AUTHOR", author);
        content = replaceAll(content, "CREATION_DATE", creationDate);
        content = replaceAll(content, "IMAGE_FILE", imageFile);
        String base = imageFile.substring(0, imageFile.indexOf('.'));
        try {
            String description = "";
            File descriptionFile = new File(fullTypeName + File.separatorChar + base + ".txt");
            if (descriptionFile.exists()) {
                FileInputStream descStream = new FileInputStream(descriptionFile);
                int len = descStream.available();
                byte descBytes[] = new byte[len];
                descStream.read(descBytes);
                description = new String(descBytes);
            } 
            content = replaceAll(content, "DESCRIPTION", description);
            String fname = base + ".html";
            PrintStream html_out = new PrintStream(new FileOutputStream(fullTypeName + "/" + fname));
            html_out.print(content);
            html_out.close();
            return fname;
        } catch(IOException ioe) {
            System.out.println("Exception writing HTML: " + ioe);
        }
        return null;
    }
    
    private static String replaceAll(String src, String key, String value) {
        while (src.indexOf(key) != -1) {
            int start = src.indexOf(key);
            src = new StringBuffer(src).replace(start, start+key.length(), value).toString();
        }
        return src;
    }
}
