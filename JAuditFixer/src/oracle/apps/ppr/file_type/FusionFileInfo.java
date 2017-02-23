package oracle.apps.ppr.file_type;

import java.io.InputStream;

import java.security.DigestInputStream;
import java.security.MessageDigest;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FusionFileInfo
{
    String jar = "---";
    String path = "---";
    String sDigest  = "---";

    
    public FusionFileInfo(ZipFile zf, ZipEntry ze)
    {
        super();
        
        jar = zf.getName();
        path = ze.getName();
        
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = zf.getInputStream(ze);
            DigestInputStream dis = new DigestInputStream(is, md);
            
            byte b[] = new byte[1000];
            while(dis.available() > 0)
                dis.read(b);
            
            byte[] digest = dis.getMessageDigest().digest();

            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<digest.length;i++) {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
                }
            
            
            sDigest = hexString.toString();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    public String getJar()
    {
        return jar;
    }

    public String getPath()
    {
        return path;
    }

    public String getDigest()
    {
        return sDigest;
    }
    
    public String toString()
    {
        return jar + " has file " + path + " with MD5 sum of " + sDigest;
    }
}
