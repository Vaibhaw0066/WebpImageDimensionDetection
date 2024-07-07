package src;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        String myDirectoryPath = "/Users/kumar-14542/Downloads/Testing_images/webp";
        File dir = new File(myDirectoryPath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {

                String p = child.getAbsolutePath();
                File img = new File(p);
                InputStream is = new FileInputStream(img);
                BufferedInputStream bis = new BufferedInputStream(is);

                System.out.print(p+"=>");
//                System.out.println(isWebpImage(bis));
//                System.out.println("   "+getDimensions(bis));
                getWebpDimension(bis);
                break;

            }
        } else {
            // Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.
        }



//        System.out.println(isWebpImage(bis));

    }

    public static boolean isWebpImage(InputStream is) throws IOException {
        if (!is.markSupported()){
            return false;
        }
        is.mark(8);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        if(c1==82 && c2==73 && c3==70 && c4==70){
            return true;
        }
        return false;
    }

    public static Map getDimensions(BufferedInputStream is) throws IOException {
        Map<String,Long> dimension = new HashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            for(Directory dir : metadata.getDirectories()){
                for (Iterator<Tag> it = dir.getTags().iterator(); it.hasNext(); ) {
                    Tag tag = it.next();
                    if(tag.hasTagName()&&tag.getTagName().contains("Height")){
                        dimension.put("height",Long.parseLong(tag.getDescription()));
                    }else if(tag.hasTagName()&&tag.getTagName().contains("Width")){
                        dimension.put("width",Long.parseLong(tag.getDescription()));
                    }
                    if(dimension.containsKey("width")&&dimension.containsKey("height")){
                        break;
                    }
                }
            }
        } catch (ImageProcessingException e) {

            System.err.println("Error extracting metadata: " + e.getMessage());
        }
        return dimension; // Couldn't determine dimensions
    }


    public static Map getWebpDimension(BufferedInputStream bis) throws IOException {

        int _chunkSize = 4;
        int _currentIdx = 0;
        byte []buff = new byte[_chunkSize];
        int count = 0;
        bis.read(buff,0, _chunkSize);
        String fileFourCC = new String(buff);

        boolean isWebpImage = false;

        if(fileFourCC!="RIFF") {
            boolean isFirstFourCC = true;
            System.out.println("===> ");
            while ((count = bis.read(buff, 0, buff.length)) != -1) {
                _currentIdx+= 4;
//                for(int i=0;i<4;i++){
////                    System.out.println((int)buff[i]);
//                }
                if(!isFirstFourCC){
                    fileFourCC = new String(buff);
                    int remlength = bis.available();

                    // This image stream CHUNK contains image width and height
                    if(fileFourCC.equals("VP8 ") && remlength > 9){
                        byte b = (byte)bis.read();
                        int chunkSize = getUInt32ForWebp(b);
                        System.out.println("chunk size : "+chunkSize+" "+b);
                        bis.mark(-1);
                        byte []_metaData = new byte[chunkSize];
                        bis.read(_metaData,0, _metaData.length);

                        int fourthByte = _metaData[3];
                        int fifthByte = _metaData[4];
                        int sixthByte = _metaData[5];
                        for(int i=0;i<100;i++){
                            System.out.println((int)_metaData[i]);
                        }
                        System.out.println(fourthByte + " , "+ fifthByte + " , " + sixthByte);

                        // ??????
                        if(fourthByte!= 157 || fifthByte!=1 || sixthByte != 42){
                            // Webp image dimension metaData can not be found
                            System.out.println("returning");
                            return null;
                        }

                        byte w1  = _metaData[6];
                        byte w2 = _metaData[7];
                        byte h1 = _metaData[8];
                        byte h2 = _metaData[9];

                        int width = getUInt16ForWebp(w1, w2);
                        int height = getUInt16ForWebp(h1, h2);

                        System.out.println("Height : "+height+", width : "+width);
                    }
                }else{
                    isFirstFourCC = false;
                }

            }
        }
        return null;
    }

    // ????
    public static int getUInt16ForWebp(byte b1, byte b2){
        return b2 << 8 & '\uff00' | b1 & 255;
    }

    // ???
    public static int getUInt32ForWebp(byte b){
        int i= b & 255 | b << 8 & '\uff00' | b << 16 & 16711680 | b << 24 & -16777216;
        return b & 255 | b << 8 & '\uff00' | b << 16 & 16711680 | b << 24 & -16777216;
    }



}