package com.yoursway.autoupdater.localrepository.internal;

import static com.google.common.collect.Maps.newHashMap;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.yoursway.autoupdater.filelibrary.FileLibraryListener;
import com.yoursway.autoupdater.filelibrary.LibraryState;
import com.yoursway.autoupdater.filelibrary.Request;
import com.yoursway.autoupdater.installer.Installation;
import com.yoursway.autoupdater.installer.InstallerException;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.LocalProductVersionMemento.State;
import com.yoursway.utils.log.Log;

class ProductVersionState_Installing extends AbstractProductVersionState implements FileLibraryListener {
    
    ProductVersionState_Installing(LocalProductVersion wrap) {
        super(wrap);
    }
    
    @Override
    public void continueWork() {
        Log.write("Ordering files.");
        listener().downloadingStarted();
        orderManager().orderChanged();
    }
    
    @Override
    public Collection<Request> libraryRequests() {
        return versionDefinition().packRequests();
    }
    
    @Override
    public void libraryChanged(LibraryState state) {
        Collection<Request> packRequests = versionDefinition().packRequests();
        if (state.filesReady(packRequests)) {
            Log.write("Files ready.");
            listener().downloadingCompleted();
            
            Collection<File> localPacks = state.getLocalFiles(packRequests);
            Map<String, File> packsMap = newHashMap();
            for (File file : localPacks) {
                String name = file.getName();
                if (!name.endsWith(".zip"))
                    throw new AssertionError("A pack file name must ends with .zip");
                String hash = name.substring(0, name.length() - 4);
                packsMap.put(hash, file);
            }
            try {
                Installation installation = new Installation(version, packsMap);
                installer().install(installation, componentStopper());
            } catch (InstallerException e) {
                e.printStackTrace(); //!
            }
        } else {
            double progress = state.localBytes(packRequests) * 1.0 / state.totalBytes(packRequests);
            listener().downloading(progress);
        }
    }
    
    @Override
    public boolean updating() {
        return true;
    }
    
    public State toMementoState() {
        return State.Installing;
    }
    
}
