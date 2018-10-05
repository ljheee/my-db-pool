package com.ljheee.pool;

import com.ljheee.pool.factory.DataSourceFactory;
import com.ljheee.pool.factory.DataSourceFactoryImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * 测试入口
 */
public class Main {
    public static void main(String[] args) throws SQLException {


        DataSourceFactory dataSourceFactory = new DataSourceFactoryImpl();
        DataSource dataSource = dataSourceFactory.getDataSource();


        // 创建连接，执行查询
        for (int i = 0; i < 10000; i++) {
            Connection connection = dataSource.getConnection();
            new TaskThread(connection).start();
            System.out.println("connection num=" + i);

        }
    }


    static class TaskThread extends Thread {

        ArrayList<Integer> list = new ArrayList<>();
        Connection connection;

        public TaskThread(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("select * from `user`");

                while (rs.next()) {
                    int id = rs.getInt(1);
                    String appID = rs.getString(2);
//                   System.out.println(id+"="+appID);
                    list.add(id);
                }
                //执行完查询事务
                System.out.println(list);

                // 使用连接池，close时，调用代理方法，将连接放回空闲队列。
                connection.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


}
