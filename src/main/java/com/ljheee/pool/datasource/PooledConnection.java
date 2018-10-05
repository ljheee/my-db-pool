package com.ljheee.pool.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接  包装类型
 */
public class PooledConnection implements InvocationHandler {

    /**
     * 标识 是否可复用
     */
    private Boolean isUsing;

    private Connection connection;
    private Connection proxyConnection;
    private PooledDataSource dataSource;



    public PooledConnection(Connection connection, PooledDataSource dataSource) {
        this.isUsing = false;
        this.connection = connection;
        this.dataSource = dataSource;
        this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class}, this);

    }

    public Boolean getUsing() {
        return isUsing;
    }

    public void setUsing(Boolean using) {
        isUsing = using;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getProxyConnection() {
        return proxyConnection;
    }

    public void setProxyConnection(Connection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();
        if ("close".hashCode() == methodName.hashCode() && "close".equals(methodName)) {
            this.setUsing(false);
            this.dataSource.pushConnection(this);// 放回空闲队列 以供复用
            return null;
        } else {
            try {
                if (!Object.class.equals(method.getDeclaringClass())) {
                    this.checkConnection();
                }

                return method.invoke(this.connection, args);
            } catch (Throwable var6) {
                throw var6;
            }
        }
    }

    private void checkConnection() throws SQLException {
        if (!this.isUsing) {
            throw new SQLException("Error accessing PooledConnection. Connection is not isUsing.");
        }
    }
}