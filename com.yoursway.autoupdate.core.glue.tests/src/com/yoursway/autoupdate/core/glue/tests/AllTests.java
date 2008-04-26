package com.yoursway.autoupdate.core.glue.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { OverallStateTests.class, AutomaticUpdatesSchedulerTests.class,
        TransactionalStorageTests.class, PersisterTests.class })
public class AllTests {
    
}
