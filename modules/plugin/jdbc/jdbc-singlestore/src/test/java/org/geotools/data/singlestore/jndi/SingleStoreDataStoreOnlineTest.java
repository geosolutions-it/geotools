package org.geotools.data.singlestore.jndi;

import org.geotools.data.singlestore.SingleStoreTestSetup;
import org.geotools.jdbc.JDBCDataStoreOnlineTest;
import org.geotools.jdbc.JDBCJNDITestSetup;
import org.geotools.jdbc.JDBCTestSetup;

public class SingleStoreDataStoreOnlineTest extends JDBCDataStoreOnlineTest {

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new JDBCJNDITestSetup(new SingleStoreTestSetup());
    }

    @Override
    protected String getCLOBTypeName() {
        // CLOB is supported in SingleStore 8 but not in 5
        return "TEXT";
    }
}
