//package com.top.db;
//
//import java.io.Serializable;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.ResultSetMetaData;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.topsoft.db.Dataset;
//import com.topsoft.rabbitmq.LoggerMQ;
//import com.topsoft.rabbitmq.LoggerManager;
//import org.mybatis.spring.SqlSessionTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import com.alibaba.fastjson.JSONObject;
//
//public  abstract class DbSqlDao implements Serializable {
//
//	/**
//	 *
//	 */
//	private static final long serialVersionUID = 1L;
//	protected Connection connection;
//	protected PreparedStatement preparedStatement;
//	protected ResultSet resultSet;
//	protected ResultSetMetaData rsmd;
//
//	protected LoggerMQ logger = LoggerManager.getLogger(null, getClass());
//
//	@Autowired private SqlSessionTemplate sessionTemplate;
//
//	public DbSqlDao(Connection connection) {
//		this.connection=connection;
//	}
//	public void setConnection(Connection connection) {
//		this.connection=connection;
//	}
//	public Connection getConnection() {
//		return connection;
//	}
//
//	public JSONObject queryRow(String sql){
//		Dataset dataset=new Dataset();
//		executeQuery(dataset,sql);
//		if (dataset.rows.size()==0)
//			return null;
//		else
//			return dataset.rows.get(0);
//	}
//	public Dataset query(String sql){
//		Dataset dataset=new Dataset();
//		executeQuery(dataset,sql);
//		return dataset;
//	}
//
//	public void executeQuery(Dataset dataset,String sql){
//		try{
//			openConnection();
//			executeQuery(sql);
//			List<JSONObject> rowsQuery=new ArrayList<JSONObject>();
//			rsmd=resultSet.getMetaData();
//			while(resultSet.next()){
//				rowsQuery.add(initRow(dataset,rsmd,resultSet));//将数据初始化到Dataset的新行
//			}
//			dataset.fill(rowsQuery);
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		} catch (InstantiationException e) {
//			throw new RuntimeException(e);
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}finally{
////			try{
////				close();
////			} catch (SQLException e) {
////				e.printStackTrace();
////			}
////			try{
////				closeConnection();
////			} catch (SQLException e) {
////				e.printStackTrace();
////			}
//		}
//	}
//	public int executeCount(String sql){
//		JSONObject row =queryRow(sql);
//		return row.getIntValue("count");
//	}
//
//	public JSONObject initRow(Dataset dataset,ResultSetMetaData resultSetMetaData, ResultSet rs) throws SQLException {
//		int count=resultSetMetaData.getColumnCount();
//		JSONObject row = new JSONObject();
////		//对列名循环
////		for(int i=1;i<=count;i++){
////			if(resultSetMetaData==null) break;
////			String fieldName=resultSetMetaData.getColumnName(i);
////			String type=resultSetMetaData.getColumnTypeName(i);//这个类型取值不准  字符串的0 会识别为 int
////			if (dataset.getFields().get(fieldName)==null) {
////				dataset.getFields().add(fieldName, new Field(fieldName, type));
////			}
////			if(resultSet.getObject(fieldName)!=null){
////				Object value;
////				if(type.equals("DATETIME")||type.equals("DATE")) {
////					value = resultSet.getDate(fieldName) + "";
////				}else {
////					value = resultSet.getObject(fieldName);
////				}
////				row.put(fieldName,value);
////			}
////		}
//
//		return row;
//	}
//
//	public void openConnection() throws Exception {
//		if (sessionTemplate!=null){
//			connection =  sessionTemplate.getConfiguration().getEnvironment().getDataSource().getConnection();
////			connection.setAutoCommit(false);
//		}
//	}
//
////	public void openStatement() throws SQLException {
////		statement = connection.createStatement();
////	}
//
//	public int executeUpdate(String sql) {
//		try {
//			preparedStatement = connection.prepareStatement(sql);
//			logger.debug(sql);
//			return preparedStatement.executeUpdate();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return 0;
//	}
//
//	public void executeQuery(String sql) throws SQLException  {
//		preparedStatement = connection.prepareStatement(sql);
//		resultSet = preparedStatement.executeQuery(sql);
//	}
//
////	public void closeConnection() throws SQLException  {
////		try {
////			if (connection != null && !connection.isClosed()) {
////				connection.close();
////			}
////		}catch(SQLException ex){
////			ex.printStackTrace();
////		}
////		finally {
////			connection = null;
////		}
////	}
//
////	public void closeStatement(){
////		try {
////			if (statement != null) {
////				statement.close();
////			}
////		} catch (SQLException e) {
////			e.printStackTrace();
////		}finally {
////			statement = null;
////		}
////	}
//
//	public void close() throws SQLException{
//		if (preparedStatement != null) {
//			preparedStatement.close();
//		}
//		if (resultSet != null) {
//			resultSet.close();
//		}
//
//		try {
//			if (connection != null && !connection.isClosed()) {
//				connection.close();
//			}
//		}catch(SQLException ex){
//			ex.printStackTrace();
//		}
//		finally {
//			connection = null;
//		}
//
//	}
//	public void rollback() throws SQLException {
//		if(connection!=null) {
//			connection.rollback();
//		}
//	}
//
//	public void commit() throws SQLException {
//		if(connection!=null) {
//			connection.commit();
//		}
//	}
//}
