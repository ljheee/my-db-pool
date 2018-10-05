package com.ljheee.pool.factory;


import com.ljheee.pool.datasource.PooledDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DataSource工厂实现类
 */
public final class DataSourceFactoryImpl implements DataSourceFactory {


    private static String driver = null;
    private static String userName = null;
    private static String password = null;
    private static String url = null;

    public static final String JDBC_LOCATION = "jdbc.properties";

    //    private static final BasicDataSource dataSource = new BasicDataSource();
    private static PooledDataSource dataSource = new PooledDataSource();

    static {
        try {
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(JDBC_LOCATION);
            Properties properties = new Properties();
            properties.load(stream);

            driver = properties.getProperty("driver");
            userName = properties.getProperty("userName");
            password = properties.getProperty("password");
            url = properties.getProperty("url");

            dataSource.setDriver(driver);
            dataSource.setPassword(password);
            dataSource.setUserName(userName);
            dataSource.setUrl(url);

            String maxActive = properties.getProperty("maxActive");
            String maxIdle = properties.getProperty("maxIdle");
            String timeToWait = properties.getProperty("timeToWait");
            if (maxActive != null) {
                dataSource.setPoolMaximumActiveConnections(Integer.parseInt(maxActive));
            }

            if (maxIdle != null) {
                dataSource.setPoolMaximumIdleConnections(Integer.parseInt(maxIdle));
            }

            if (timeToWait != null) {
                dataSource.setPoolTimeToWait(Integer.parseInt(timeToWait));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

}
