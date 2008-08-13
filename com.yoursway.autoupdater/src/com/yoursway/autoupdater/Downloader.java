package com.yoursway.autoupdater;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.yoursway.autoupdater.auxiliary.DownloadTask;
import com.yoursway.autoupdater.auxiliary.DownloadTaskItem;

public class Downloader {
    
    private final String place;
    
    public Downloader(String place) {
        this.place = place;
        
        //> add slash after place
    }
    
    public void download(DownloadTask task) {
        for (DownloadTaskItem item : task.items())
            download(item);
    }
    
    private void download(DownloadTaskItem item) {
        InputStream in = null;
        OutputStream out = null;
        
        try {
            in = new URL(item.url()).openStream();
            out = new BufferedOutputStream(new FileOutputStream(place + item.filename()));
            
            byte[] buffer = new byte[1024];
            int read;
            long written = 0;
            while (true) {
                read = in.read(buffer);
                if (read == -1)
                    return;
                
                out.write(buffer, 0, read);
                written += read;
            }
            
            //> check file size
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
    }
}