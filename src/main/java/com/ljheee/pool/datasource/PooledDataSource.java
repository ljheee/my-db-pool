package com.ljheee.pool.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 连接池  DataSource实现类
 * 维护 活跃连接队列 和 空闲队列
 */
public class PooledDataSource implements DataSource {


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

    private PoolState poolState = new PoolState();

    public PooledDataSource() {
    }

    public PooledDataSource(String driver, String url, String userName, String password) {
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
        return doGetConnection(this.userName, this.password).getProxyConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password).getProxyConnection();
    }

    /**
     * 获取新连接
     * 优先从 空闲队列获取
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    private PooledConnection doGetConnection(String username, String password) throws SQLException {
        this.initializeDriver();
        PooledConnection conn = null;

        while (conn == null) {
            synchronized (this.poolState) {
                if (!this.poolState.idleConnections.isEmpty()) {
                    // 有空闲 连接
                    conn = this.poolState.idleConnections.remove(0);
                } else if (this.poolState.activeConnections.size() < this.poolMaximumActiveConnections) {
                    //无空闲连接，但可以创建新连接
                    conn = new PooledConnection(newConnection(), this);
                } else {

                    //获取 最旧 的连接，看是否可以复用
                    PooledConnection oldestActiveConnection = this.poolState.activeConnections.get(0);
                    if (!oldestActiveConnection.getUsing()) {
                        conn = new PooledConnection(oldestActiveConnection.getConnection(), this);
                        this.poolState.activeConnections.remove(oldestActiveConnection);
                    } else {
                        //没有可复用的，且不能再创建新的
                        try {
                            //调用wait，导致当前线程（用T表示）阻塞，释放锁，进入等待状态
                            //等待时间（timeout）后，自动唤醒
                            this.poolState.wait((long) this.poolTimeToWait);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

                if (conn != null) {
                    if (!conn.getUsing()) {
                        if (!conn.getConnection().getAutoCommit()) {
                            conn.getConnection().rollback();
                        }
                        this.poolState.activeConnections.add(conn);
                    }
                }
            }
        }
        // 标识 该连接也被客户端使用，暂不可复用
        conn.setUsing(true);
        return conn;
    }

    private Connection newConnection() throws SQLException {
        this.initializeDriver();
        Connection connection = DriverManager.getConnection(this.url, this.userName, this.password);
        return connection;
    }


    /**
     * 将连接 放回 idleConnections
     *
     * @param conn
     * @throws SQLException
     */
    protected void pushConnection(PooledConnection conn) throws SQLException {
        synchronized (this.poolState) {
            this.poolState.activeConnections.remove(conn);
            if (this.poolState.idleConnections.size() < this.poolMaximumIdleConnections) {
                if (!conn.getConnection().getAutoCommit()) {
                    conn.getConnection().rollback();
                }

                PooledConnection newConn = new PooledConnection(conn.getConnection(), this);
                this.poolState.idleConnections.add(newConn);

                this.poolState.notifyAll();
            } else {
                if (!conn.getConnection().getAutoCommit()) {
                    conn.getConnection().rollback();
                }

                conn.getConnection().close();
            }
        }
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
