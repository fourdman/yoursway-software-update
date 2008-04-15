package com.yoursway.autoupdate.core;

import static com.yoursway.autoupdate.core.FileStateBuilder.buildActions;
import static com.yoursway.autoupdate.core.FileStateBuilder.modifiedFiles;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.eclipse.core.runtime.Platform;

import com.yoursway.autoupdate.core.execution.RealExecutor;
import com.yoursway.autoupdate.core.execution.RealExecutor9;
import com.yoursway.autoupdate.core.execution.RealReplaceTester;
import com.yoursway.autoupdate.core.versions.Version;
import com.yoursway.autoupdate.core.versions.definitions.IVersionDefinitionLoader;
import com.yoursway.autoupdate.core.versions.definitions.InvalidVersionDefinitionException;
import com.yoursway.autoupdate.core.versions.definitions.RemoteFile;
import com.yoursway.autoupdate.core.versions.definitions.UpdaterInfo;
import com.yoursway.autoupdate.core.versions.definitions.UrlBasedVersionDefinitionLoader;
import com.yoursway.autoupdate.core.versions.definitions.VersionDefinition;
import com.yoursway.autoupdate.core.versions.definitions.VersionDefinitionNotAvailable;

public class AutomaticUpdater {
    
    public static void checkForUpdates(Version currentVersion, URL defaultUrl) throws UpdatesFoundExit {
        File location = new File(Platform.getInstallLocation().getURL().getPath());
        ApplicationInstallation install = new ApplicationInstallation(location);
        checkForUpdates(install, currentVersion, defaultUrl);
    }

    public static void checkForUpdates(ApplicationInstallation install, Version currentVersion,
            URL defaultUrl) throws UpdatesFoundExit {
        String overrideUrl = System.getProperty("updater.url.override");
        if (overrideUrl != null)
            try {
                defaultUrl = new URL(overrideUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        IVersionDefinitionLoader loader = new UrlBasedVersionDefinitionLoader(defaultUrl);
        try {
            VersionDefinition currentDef = loader.loadDefinition(currentVersion);
            if (!currentDef.hasNewerVersion())
                return;
            
            Version freshVersion = currentDef.nextVersion();
            VersionDefinition freshDef = loader.loadDefinition(freshVersion);
            
            Collection<RemoteFile> freshFiles = freshDef.files();
            
            Collection<FileAction> actions = buildActions(install.getFileContainer(), freshFiles);
            
            RealExecutor executor = new RealExecutor();
            RealReplaceTester replaceTester = new RealReplaceTester();
            RealExecutor9 executor9 = new RealExecutor9();
            
            UpdaterInfo updaterInfo = freshDef.updaterInfo();
            UpdatePlanBuilder planBuilder = new UpdatePlanBuilder(replaceTester, modifiedFiles(actions)
                    .asCollection(), updaterInfo.files());
            UpdatePlan plan = planBuilder.build();
            ExecutablePlan executablePlan = plan.instantiate(new UpdateRequest(new File("/IDE"), install
                    .getFileContainer().allFiles(), actions, updaterInfo, executor9));
            executablePlan.execute(executor);
            
            throw new UpdatesFoundExit();
        } catch (VersionDefinitionNotAvailable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidVersionDefinitionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
}