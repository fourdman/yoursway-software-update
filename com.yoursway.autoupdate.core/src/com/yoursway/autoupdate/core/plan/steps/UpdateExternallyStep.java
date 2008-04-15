package com.yoursway.autoupdate.core.plan.steps;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.util.List;

import com.yoursway.autoupdate.core.UpdateRequest;
import com.yoursway.autoupdate.core.actions.Action;
import com.yoursway.autoupdate.core.actions.concrete.RemoveRecursivelyAction;
import com.yoursway.autoupdate.core.actions.concrete.StartMainEclipseAction;
import com.yoursway.autoupdate.core.actions.concrete.UpdateExternallyAction;
import com.yoursway.autoupdate.core.plan.dirs.Directory;
import com.yoursway.utils.filespec.FileSetSpec;

public class UpdateExternallyStep implements UpdateStep {
    
    private final Directory updaterLocation;
    private final FileSetSpec files;
    
    public UpdateExternallyStep(Directory updaterLocation, FileSetSpec files) {
        this.updaterLocation = updaterLocation;
        this.files = files;
    }
    
    @Override
    public String toString() {
        return "RESTART FROM " + updaterLocation + " UPDATE " + files;
    }
    
    public void createActions(UpdateRequest request, List<Action> storeInto) {
        File loc = request.resolve(updaterLocation);
        List<Action> cleanup = newArrayList();
        if (updaterLocation.isTemporary())
            cleanup.add(new RemoveRecursivelyAction(loc));

        List<Action> pending = newArrayList();
        pending.addAll(request.resolveUpdate(files));
        pending.add(new StartMainEclipseAction(request.determineCurrentEclipseStartInfo(), cleanup));
        storeInto.add(new UpdateExternallyAction(loc, request.resolveUpdaterJar(loc), pending));
    }
    
}