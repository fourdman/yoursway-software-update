package com.yoursway.autoupdater.gui.demo;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.yoursway.autoupdater.core.auxiliary.AutoupdaterException;
import com.yoursway.autoupdater.core.auxiliary.SuiteDefinition;
import com.yoursway.autoupdater.core.auxiliary.UpdatableApplication;
import com.yoursway.autoupdater.core.auxiliary.UpdatableApplicationProductFeatures;
import com.yoursway.autoupdater.core.auxiliary.UpdatableApplicationView;
import com.yoursway.autoupdater.core.localrepository.LocalRepository;
import com.yoursway.autoupdater.gui.controller.UpdaterController;
import com.yoursway.autoupdater.gui.view.UpdaterView;
import com.yoursway.autoupdater.gui.view.UpdaterViewFactory;
import com.yoursway.utils.YsFileUtils;

public class UpdaterDemo {
    
    private static Shell shell;
    
    public static void main(final String[] args) {
        final Display display = new Display();
        
        shell = new Shell(display);
        shell.setText("Autoupdater");
        shell.setBounds(new Rectangle(480, 320, 320, 240));
        
        UpdatableApplication app = new UpdatableApplication() {
            
            public String updateSite() {
                return args[0];
            }
            
            public String suiteName() {
                return args[1];
            }
            
            public boolean inInstallingState() {
                return false;
            }
            
            public void setInstallingState(boolean value) {
                // nothing
            }
            
            public UpdatableApplicationProductFeatures getFeatures(String productName) {
                return UpdatableApplicationProductFeatures.MOCK;
            }
            
            public UpdatableApplicationView view() {
                return new UpdatableApplicationView() {
                    public void displayAutoupdaterErrorMessage(AutoupdaterException e) {
                        fatalError(e);
                    }
                };
            }
            
            public File localRepositoryPlace() throws IOException {
                return YsFileUtils.createTempFolder("autoupdater-demo-repo-", null);
            }
            
        };
        
        UpdaterViewFactory viewFactory = new UpdaterViewFactory() {
            public UpdaterView createView(UpdatableApplicationView appView, SuiteDefinition suite,
                    LocalRepository repo) {
                return new UpdaterView(shell, appView, suite, repo, new UpdaterStyleMock(display));
            }
        };
        UpdaterController controller = new UpdaterController(app, viewFactory);
        controller.updateApplication();
        
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch())
                    display.sleep();
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
        }
        
        display.dispose();
    }
    
    private static void fatalError(Throwable e) {
        MessageBox msg = new MessageBox(shell, SWT.NONE);
        StringBuilder sb = new StringBuilder();
        for (Throwable _e = e; _e != null; _e = _e.getCause())
            sb.append(e.getClass().getSimpleName() + ": " + e.getMessage() + ".\n");
        msg.setMessage(sb.toString());
        msg.open();
        System.exit(-1);
    }
}
