package my.company.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.MessageFormat;

public class StoredProcEmbedded {
    private static final Logger log = LoggerFactory.getLogger(StoredProcEmbedded.class);
    public static final String STATUS_ERROR = "STATUS_ERROR";

    public static void GETZIPS(String city,
                               //OUT - array is used as a "reference pointer", but only item[0] is expected
                               int[] status,
                               String[] message,
                               //A stored procedure can return multiple result sets
                               ResultSet[] resultSets) throws SQLException {
        log.debug("GETZIPS is called. city={}",city);

        //To test status error handling
        if (STATUS_ERROR.equals(city)) {
            status[0] = 1;
            message[0] = "Unexpected city";
            return;
        }

        //Regular respose from table
        status[0] = 0;
        message[0] = "OK";

        //Create a resultset by querying from the table
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement statement = conn.prepareStatement(MessageFormat.format("select * from GETZIPS_RESPONSE where CITY=''{0}'' order by ZIP", city));
        resultSets[0] = statement.executeQuery();
    }
}
