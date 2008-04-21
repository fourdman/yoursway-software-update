package com.yoursway.autoupdate.core;

import static com.yoursway.autoupdate.core.FileStateBuilder.buildActions;
import static com.yoursway.autoupdate.core.FileStateBuilder.modifiedFiles;
import static com.yoursway.autoupdate.core.internal.Activator.log;
import static com.yoursway.utils.YsFileUtils.saveToFile;
import static com.yoursway.utils.YsFileUtils.urlToFileWithProtocolCheck;

import java.io.File;
import java.io.IOException;
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
import com.yoursway.utils.StringInputStream;

public class AutomaticUpdater {
    
    private static final String PROPERTY_URL_OVERRIDE = "updater.url.override";
    
    private static final String PROPERTY_TESTS_PING_URL = "updater.tests.ping.url";
    
    public static void checkForUpdates(Version currentVersion, URL defaultUrl) throws UpdatesFoundExit {
        ApplicationInstallation install = new ApplicationInstallation(urlToFileWithProtocolCheck(Platform
                .getInstallLocation().getURL()));
        checkForUpdates(install, currentVersion, defaultUrl);
    }
    
    public static void checkForUpdates(ApplicationInstallation install, Version currentVersion, URL updateUrl)
            throws UpdatesFoundExit {
        if (!forcedUpdateCheckBecauseOfTests() && !shouldCheckForUpdates())
            return;
        updateUrl = determineUpdateUrl(updateUrl);
        IVersionDefinitionLoader loader = new UrlBasedVersionDefinitionLoader(updateUrl);
        try {
            VersionDefinition currentDef = loader.loadDefinition(currentVersion);
            if (!currentDef.hasNewerVersion()) {
                pingTestRunnerAndExitIfRequested();
                return;
            }
            
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
            try {
                saveToFile(new StringInputStream(plan.toString()), new File("/tmp/plan.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            ExecutablePlan executablePlan = plan.instantiate(new UpdateRequest(install.root(), install
                    .getFileContainer().allFiles(), actions, updaterInfo, executor9));
            executablePlan.execute(executor);
            
            throw new UpdatesFoundExit(true);
        } catch (VersionDefinitionNotAvailable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidVersionDefinitionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static boolean shouldCheckForUpdates() {
        // TODO: once a day etc
        return true;
    }

    private static boolean forcedUpdateCheckBecauseOfTests() {
        return System.getProperty(PROPERTY_TESTS_PING_URL) != null;
    }
    
    private static void pingTestRunnerAndExitIfRequested() throws UpdatesFoundExit {
        String pingUrl = System.getProperty(PROPERTY_TESTS_PING_URL);
        if (pingUrl != null) {
            try {
                URL url = new URL(pingUrl);
                url.openStream().close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new UpdatesFoundExit(false);
        }
    }
    
    private static URL determineUpdateUrl(URL updateUrl) {
        String overrideUrl = System.getProperty(PROPERTY_URL_OVERRIDE);
        if (overrideUrl != null)
            try {
                log("Override update URL is " + overrideUrl);
                return new URL(overrideUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        return updateUrl;
    }
    
}
