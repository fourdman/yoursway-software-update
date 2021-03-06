package com.yoursway.autoupdater.core.filelibrary;

import static com.yoursway.utils.DebugOutputHelper.reflectionBasedToString;

import java.net.MalformedURLException;
import java.net.URL;

import com.yoursway.autoupdater.core.protos.LocalRepositoryProtos.RequestMemento;

public class Request {
    
    private final URL url;
    final long size;
    private final String sha1;
    
    public Request(URL url, long size, String sha1) {
        if (url == null)
            throw new NullPointerException("url is null");
        if (sha1 == null)
            throw new NullPointerException("sha1 is null");
        
        this.url = url;
        this.size = size;
        this.sha1 = sha1;
    }
    
    @Override
    public String toString() {
        return reflectionBasedToString(this);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sha1 == null) ? 0 : sha1.hashCode());
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Request other = (Request) obj;
        if (sha1 == null) {
            if (other.sha1 != null)
                return false;
        } else if (!sha1.equals(other.sha1))
            return false;
        if (size != other.size)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }
    
    public static Request fromMemento(RequestMemento memento) throws MalformedURLException {
        URL url = new URL(memento.getUrl());
        return new Request(url, memento.getSize(), memento.getSha1());
    }
    
    public RequestMemento toMemento() {
        return RequestMemento.newBuilder().setUrl(url.toString()).setSize(size).setSha1(sha1).build();
    }
    
    public URL url() {
        return url;
    }
    
    public String hash() {
        return sha1;
    }
    
}
