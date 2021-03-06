package com.yoursway.autoupdater.core.localrepository.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import com.yoursway.autoupdater.core.auxiliary.AutoupdaterException;
import com.yoursway.autoupdater.core.auxiliary.ComponentStopper;
import com.yoursway.autoupdater.core.auxiliary.ErrorsAggregator;
import com.yoursway.autoupdater.core.auxiliary.ErrorsListener;
import com.yoursway.autoupdater.core.auxiliary.ProductDefinition;
import com.yoursway.autoupdater.core.auxiliary.ProductVersionDefinition;
import com.yoursway.autoupdater.core.auxiliary.UpdatableApplicationProductFeatures;
import com.yoursway.autoupdater.core.auxiliary.UpdatableApplicationProductFeaturesProvider;
import com.yoursway.autoupdater.core.filelibrary.FileLibrary;
import com.yoursway.autoupdater.core.filelibrary.OrderManager;
import com.yoursway.autoupdater.core.installer.Installer;
import com.yoursway.autoupdater.core.localrepository.LocalRepositoryChangerCallback;
import com.yoursway.autoupdater.core.localrepository.UpdatingListener;
import com.yoursway.autoupdater.core.protos.LocalRepositoryProtos.LocalProductMemento;
import com.yoursway.autoupdater.core.protos.LocalRepositoryProtos.LocalProductVersionMemento;
import com.yoursway.autoupdater.core.protos.LocalRepositoryProtos.LocalProductMemento.Builder;
import com.yoursway.utils.EventSource;
import com.yoursway.utils.annotations.Nullable;
import com.yoursway.utils.log.Log;

public class LocalProduct {
    
    private final ProductDefinition definition;
    
    private final Map<ProductVersionDefinition, LocalProductVersion> versions = new HashMap<ProductVersionDefinition, LocalProductVersion>();
    
    private final FileLibrary fileLibrary;
    final OrderManager orderManager;
    final Installer installer;
    private final LocalRepositoryChangerCallback lrcc;
    
    private final UpdatableApplicationProductFeatures features;
    
    private final ErrorsAggregator errors = new ErrorsAggregator();
    
    public LocalProduct(LocalProductMemento memento, FileLibrary fileLibrary, Installer installer,
            UpdatableApplicationProductFeaturesProvider featuresProvider, LocalRepositoryChangerCallback lrcc) {
        this.orderManager = fileLibrary.orderManager();
        this.fileLibrary = fileLibrary;
        this.installer = installer;
        this.lrcc = lrcc;
        
        try {
            definition = ProductDefinition.fromMemento(memento.getDefinition());
        } catch (MalformedURLException e) {
            e.printStackTrace(); //!
            throw new RuntimeException(e);
        }
        
        for (LocalProductVersionMemento m : memento.getVersionList()) {
            try {
                LocalProductVersion version = LocalProductVersion.fromMemento(m, this, lrcc);
                add(version);
            } catch (MalformedURLException e) {
                e.printStackTrace(); //!
            }
        }
        
        features = featuresProvider.getFeatures(definition.name());
    }
    
    public LocalProduct(ProductDefinition definition, FileLibrary fileLibrary, Installer installer,
            UpdatableApplicationProductFeaturesProvider featuresProvider, LocalRepositoryChangerCallback lrcc) {
        
        if (definition == null)
            throw new NullPointerException("definition is null");
        if (fileLibrary == null)
            throw new NullPointerException("fileLibrary is null");
        if (installer == null)
            throw new NullPointerException("installer is null");
        if (lrcc == null)
            throw new NullPointerException("lrcc is null");
        
        this.orderManager = fileLibrary.orderManager();
        this.fileLibrary = fileLibrary;
        this.installer = installer;
        this.lrcc = lrcc;
        
        this.definition = definition;
        
        features = featuresProvider.getFeatures(definition.name());
    }
    
    public EventSource<ErrorsListener> errors() {
        return errors;
    }
    
    public void startUpdating(ProductVersionDefinition versionDefinition, @Nullable UpdatingListener listener) {
        if (updating())
            throw new IllegalStateException("Updating of the product has started already.");
        
        Log.write("Starting updating to version " + versionDefinition);
        
        LocalProductVersion localVersion = versions.get(versionDefinition);
        if (localVersion != null) {
            if (listener != null)
                localVersion.events().addListener(listener);
            localVersion.startUpdating();
        } else {
            localVersion = new LocalProductVersion(this, versionDefinition, lrcc);
            if (listener != null)
                localVersion.events().addListener(listener);
            add(localVersion);
            localVersion.continueWork();
        }
    }
    
    private void add(LocalProductVersion version) {
        versions.put(version.definition(), version);
        lrcc.localRepositoryChanged();
        fileLibrary.events().addListener(version);
        orderManager.register(version);
        version.errors.addListener(errors);
    }
    
    private boolean updating() {
        for (LocalProductVersion version : versions.values())
            if (version.updating())
                return true;
        return false;
    }
    
    public void atStartup() {
        for (LocalProductVersion version : versions.values())
            try {
                version.atStartup();
            } catch (AutoupdaterException e) {
                errors.errorOccured(e);
            }
    }
    
    public void continueWork() {
        for (LocalProductVersion version : versions.values())
            version.continueWork();
    }
    
    public ProductDefinition definition() {
        return definition;
    }
    
    public LocalProductMemento toMemento() {
        Builder b = LocalProductMemento.newBuilder().setDefinition(definition.toMemento());
        for (LocalProductVersion version : versions.values())
            b.addVersion(version.toMemento());
        return b.build();
    }
    
    public ProductVersionDefinition currentVersion() throws DefinitionException {
        try {
            String vdPath = features.currentVersionDefinitionPath();
            File vdFile = new File(rootFolder(), vdPath);
            return ProductVersionDefinition.loadFrom(vdFile.toURL(), definition);
        } catch (Throwable e) {
            throw new DefinitionException("Cannot load the current version definition", e);
        }
    }
    
    public ComponentStopper componentStopper() {
        return features.componentStopper();
    }
    
    public File rootFolder() throws IOException {
        return features.rootFolder();
    }
    
    public String executablePath() {
        return features.executablePath();
    }
    
    public LocalProductVersion getLocalVersion(ProductVersionDefinition version) {
        return versions.get(version);
    }
    
}
