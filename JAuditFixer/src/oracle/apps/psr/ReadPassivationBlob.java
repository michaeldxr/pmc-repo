package oracle.apps.psr;

import java.io.BufferedOutputStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;

import java.io.FileWriter;
import java.io.InputStream;

import java.io.PrintStream;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReadPassivationBlob {
   
    public static void main(String[] args) throws Exception{
        
        Connection con = DriverManager.getConnection("jdbc:oracle:thin:@rws66175fwks.us.oracle.com:1522:rws66175", "fusion", "fusion");
        con.setAutoCommit(false);
        
        FileOutputStream fos = new FileOutputStream(new File("passivatedAMs8.txt"));
      //  BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File("passivatedAMs2.txt")));
        PrintStream ps = new PrintStream(fos);
        
        Statement stmt = con.createStatement();
        String sql = "SELECT content FROM ps_txn WHERE creation_date >= TO_DATE('21-MAR-12')";
      //  long offset = 0;
        
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()){
            ps.println();
            ps.println();
            ps.println();
            ps.println();
            ps.println();
            ps.flush();
            Blob b = rs.getBlob(1);
            //System.out.println(b.getBytes(1,(int)b.length()));
            InputStream is = b.getBinaryStream();
            byte[] buffer = new byte[(int)b.length()];
            int length = -1;
            while((length = is.read(buffer)) != -1) {
                fos.write(buffer,0,length);
                fos.flush();
            }
//            bos.write(b.getBytes(1, (int)b.length()),0,(int)b.length());
//            bos.flush();
            b.free();
        }
        
        fos.close();
       // bos.close();
        rs.close();
        con.commit();
        con.close();
        ps.close();
    }
}
