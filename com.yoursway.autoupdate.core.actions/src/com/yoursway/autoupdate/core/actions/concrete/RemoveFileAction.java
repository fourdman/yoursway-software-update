/**
 * 
 */
package com.yoursway.autoupdate.core.actions.concrete;

import java.io.File;

import com.yoursway.autoupdate.core.actions.Action;
import com.yoursway.autoupdate.core.actions.Executor;

public final class RemoveFileAction implements Action {
    
    private static final long serialVersionUID = 1L;

	private final File file;

    public RemoveFileAction(File file) {
        this.file = file;
	}

    @Override
    public String toString() {
        return "DELETE " + file;
    }

    public void execute(Executor executor) {
        executor.deleteFile(file);
    }
	
}
