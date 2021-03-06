package com.yoursway.autoupdater.installer.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.yoursway.autoupdater.core.installer.log.InstallerLog;

public class SWTView implements InstallerView {
    
    private final Display display;
    private final Shell shell;
    
    private final StyledText styledText;
    private final Color errorColor;
    
    private boolean errors = false;
    
    public SWTView() {
        display = new Display();
        
        shell = new Shell(display);
        shell.setText("Installer Log");
        shell.setBounds(240, 240, 480, 320);
        shell.setLayout(new FillLayout());
        
        styledText = new StyledText(shell, SWT.V_SCROLL | SWT.H_SCROLL);
        styledText.setEditable(false);
        errorColor = new Color(styledText.getDisplay(), 255, 0, 0);
        
        //> forbid window closing
        
        shell.open();
    }
    
    public InstallerLog getLog() {
        return new InstallerLog() {
            
            public void debug(final String msg) {
                styledText.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        styledText.append(msg + "\n");
                    }
                });
            }
            
            public void error(final String msg) {
                errors = true;
                styledText.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        StyleRange sr = new StyleRange();
                        sr.start = styledText.getCharCount();
                        sr.length = msg.length();
                        sr.foreground = errorColor;
                        
                        styledText.append(msg + "\n");
                        styledText.setStyleRange(sr);
                    }
                });
            }
            
            public void error(Throwable e) {
                error(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            
        };
    }
    
    public void doMessageLoop() {
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
    
    public void done() {
        //> allow user to close the window
        
        if (!errors) {
            display.asyncExec(new Runnable() {
                public void run() {
                    shell.close();
                }
            });
        }
    }
}
