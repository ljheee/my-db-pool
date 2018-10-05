package com.ljheee.pool.datasource;

import java.util.ArrayList;
import java.util.List;

/**
 * 维护 活跃连接队列 和 空闲队列
 */
public class PoolState {

    protected final List<PooledConnection> idleConnections = new ArrayList();
    protected final List<PooledConnection> activeConnections = new ArrayList();


}
