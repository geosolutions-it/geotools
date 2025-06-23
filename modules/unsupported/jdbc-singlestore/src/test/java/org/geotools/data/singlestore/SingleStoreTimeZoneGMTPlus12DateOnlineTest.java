package org.geotools.data.singlestore;

import java.util.TimeZone;
import org.geotools.jdbc.JDBCDateTestSetup;
import org.geotools.jdbc.JDBCTimeZoneDateOnlineTest;

public class SingleStoreTimeZoneGMTPlus12DateOnlineTest extends JDBCTimeZoneDateOnlineTest {

    @Override
    protected JDBCDateTestSetup createTestSetup() {
        super.setTimeZone(TimeZone.getTimeZone("Etc/GMT+12"));
        return new SingleStoreDateTestSetup();
    }
}
