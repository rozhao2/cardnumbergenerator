package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataBaseHelper {

    private SectionProperties prop = SectionPropertiesFactory.getProperties();
    private static final Log LOG = LogFactory.getLog(DataBaseHelper.class);

    public Connection getConnection() {
        try {
            return getIndependentConnection();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private Connection getIndependentConnection() throws SQLException, ClassNotFoundException {
        String url = "jdbc:sqlite:" + prop.getValue("COMMON", "SQLITE_FILE");
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(url);
    }

    private Connection getIndependentConnection(String databaseType) throws SQLException, ClassNotFoundException {

        if ("ORACLE".equalsIgnoreCase(databaseType)) {

            String url = "jdbc:oracle:thin:@" + prop.getValue("COMMON", "DB_SERVER") + ":"
                    + prop.getValue("COMMON", "DB_PORT") + ":" + prop.getValue("COMMON", "DB_SID");
            String user = prop.getValue("COMMON", "DB_USER");
            String password = prop.getValue("COMMON", "DB_PASSWD");
            String driver = "oracle.jdbc.driver.OracleDriver";

            Class.forName(driver);
            return DriverManager.getConnection(url, user, password);
        }

        if ("SQLITE".equalsIgnoreCase(databaseType)) {
            String url = "jdbc:sqlite:" + prop.getValue("COMMON", "SQLITE_FILE");
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(url);
        }

        return null;
    }

    public int executeUpdate(String strQuery) {
        this.debugQuery(strQuery);
        ArrayList<String> bindNames = new ArrayList<String>();
        return executeUpdate(strQuery, bindNames, false);
    }

    public int executeUpdate(String strQuery, List<String> bindNames) {
        return executeUpdate(strQuery, bindNames, false);
    }

    private int executeUpdate(String strQuery, List<String> bindNames, boolean returnLastId) {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            debugQuery(strQuery, bindNames);
            pstmt = conn.prepareStatement(strQuery);

            int ii = 1;
            for (int i = 0; i < bindNames.size(); i++) {
                String value = bindNames.get(i);
                pstmt.setString(ii++, value);
            }

            if (returnLastId) {
                if (pstmt.executeUpdate() > 0) {

                    strQuery = "SELECT LAST_INSERT_ID() AS ID";
                    debugQuery(strQuery);
                    pstmt = conn.prepareStatement(strQuery);

                    rs = pstmt.executeQuery();
                    rs.next();
                    return rs.getInt("ID");
                } else {
                    return 0;
                }
            } else {
                return pstmt.executeUpdate();
            }

        } catch (Exception ex) {
            debugQueryException(ex, strQuery);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DataBaseHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return 0;
    }

    public LinkedHashMap<String, String> getData(String strQuery) {
        this.debugQuery(strQuery);
        ArrayList<String> bindNames = new ArrayList<String>();
        return getData(strQuery, bindNames);
    }

    public LinkedHashMap<String, String> getData(String strQuery, List<String> bindNames) {
        this.debugQuery(strQuery, bindNames);
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        LOG.debug(strQuery);
        LOG.debug(StringUtils.join(bindNames, ","));
        try {
            conn = getConnection();

            debugQuery(strQuery, bindNames);
            pstmt = conn.prepareStatement(strQuery);

            int ii = 1;
            for (int i = 0; i < bindNames.size(); i++) {
                String value = bindNames.get(i);
                pstmt.setString(ii++, value);
            }

            rs = pstmt.executeQuery();
            if (!rs.next()) {
                return data;
            }

            rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {

                if (rs.getString(i) == null) {
                    data.put(rsmd.getColumnLabel(i), "");
                } else {
                    data.put(rsmd.getColumnLabel(i), rs.getString(i));
                }
            }

        } catch (Exception ex) {
            debugQueryException(ex, strQuery);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DataBaseHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return data;
    }

    public List<Map<String, String>> getDataList(String strQuery) {
        this.debugQuery(strQuery);
        ArrayList<String> bindNames = new ArrayList<String>();
        return getDataList(strQuery, bindNames);
    }

    public List<Map<String, String>> getDataList(String strQuery, ArrayList<String> bindNames, int totalCount) {
        this.debugQuery(strQuery, bindNames);
        List<Map<String, String>> data = getDataList(strQuery, bindNames);

        if (totalCount == 0) {
            totalCount = data.size();
        }

        if (totalCount < 0) {
            return data;
        }

        for (int i = 0; i < data.size(); i++) {
            data.get(i).put("OrderIndex", Integer.toString(totalCount--));
        }

        return data;
    }

    public List<Map<String, String>> getDataList(String strQuery, List<String> bindNames) {
        this.debugQuery(strQuery, bindNames);
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;

        try {
            conn = getConnection();

            debugQuery(strQuery, bindNames);

            pstmt = conn.prepareStatement(strQuery);

            int ii = 1;
            for (int i = 0; i < bindNames.size(); i++) {
                String value = bindNames.get(i);
                pstmt.setString(ii++, value);
            }

            rs = pstmt.executeQuery();
            rsmd = rs.getMetaData();
            while (rs.next()) {
                LinkedHashMap<String, String> row = new LinkedHashMap<String, String>();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {

                    if (rs.getString(i) == null) {
                        row.put(rsmd.getColumnLabel(i), "");
                    } else {
                        row.put(rsmd.getColumnLabel(i), rs.getString(i));
                    }
                }
                data.add(row);
            }

        } catch (Exception ex) {
            debugQueryException(ex, strQuery);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DataBaseHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return data;
    }

    private void debugQuery(String strQuery) {
        LOG.debug(String.format("\nstrQuery : %s\n", strQuery));
    }

    private void debugQuery(String strQuery, List<String> bindNames) {
        String errorMsg = "";
        errorMsg += String.format("\n%s\n", strQuery);
        for (int i = 0; i < bindNames.size(); i++) {
            String value = bindNames.get(i);
            errorMsg += String.format("    param = %s\n", value);
        }
        LOG.debug(errorMsg);
    }

    private void debugQueryException(Exception ex, String strQuery) {
        ex.printStackTrace();
        String errorMessage = String.format("\nDB Exception : strQuery : %s\nMessage : %s\n", strQuery, ex.getMessage());
        // System.err.println(errorMessage);
        LOG.debug(errorMessage);
    }

    public static void main(String[] args) {
        DataBaseHelper helper = new DataBaseHelper();
        Map<String, String> result = helper.getData("select * from t_area_code");
        for (String key : result.keySet()) {
            System.out.println(key + " = " + result.get(key));
        }
    }
}
