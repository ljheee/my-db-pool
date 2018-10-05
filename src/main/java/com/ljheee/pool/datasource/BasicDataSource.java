package com.ljheee.pool.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 非池化 DataSource实现类
 * 每次获取 连接 都创建新的
 */
public class BasicDataSource implements DataSource {


    private String driver = null;
    private String userName = null;
    private String password = null;
    private String url = null;

    //数据源的实现，指定最大连接数
    private int poolMaximumActiveConnections = 5;
    private int poolTimeToWait = 20000;
    private int poolMaximumIdleConnections = 10;


    // 保存 已注册的驱动
    private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap();

    public BasicDataSource() {
    }

    public BasicDataSource(String driver, String url, String userName, String password) {
        this.driver = driver;
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPoolMaximumActiveConnections() {
        return poolMaximumActiveConnections;
    }

    public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
        this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    }

    public int getPoolTimeToWait() {
        return poolTimeToWait;
    }

    public void setPoolTimeToWait(int poolTimeToWait) {
        this.poolTimeToWait = poolTimeToWait;
    }

    public int getPoolMaximumIdleConnections() {
        return poolMaximumIdleConnections;
    }

    public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
        this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(this.userName, this.password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password);
    }

    private Connection doGetConnection(String username, String password) throws SQLException {
        this.initializeDriver();
        Connection connection = DriverManager.getConnection(url, username, password);
        return connection;
    }

    private synchronized void initializeDriver() throws SQLException {

        // 如果已经注册驱动，无需每次都加载
        if (!registeredDrivers.containsKey(this.driver)) {
            try {
                Class driverType = Class.forName(driver);
                Driver driverInstance = (Driver) driverType.newInstance();
                DriverManager.registerDriver(driverInstance);
                registeredDrivers.put(this.driver, driverInstance);
            } catch (Exception var3) {
                throw new SQLException("Error setting driver on BasicDataSource. Cause: " + var3);
            }
        }
    }


    /**
     * 下面这些 是实现接口的方法
     *
     * @param iface
     * @param <T>
     * @return
     * @throws SQLException
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(this.getClass().getName() + " is not a wrapper.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("global");
    }
}
