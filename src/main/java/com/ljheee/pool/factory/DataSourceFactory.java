package com.ljheee.pool.factory;

import javax.sql.DataSource;

/**
 *  DataSource 工厂接口
 */
public interface DataSourceFactory {
    DataSource getDataSource();
}
