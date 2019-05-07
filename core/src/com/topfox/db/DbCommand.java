//package com.top.db;
//
//import java.sql.Connection;
//import com.alibaba.fastjson.JSONObject;
//import com.top.db.DbSqlDao;
//
//public class DbCommand extends DbSqlDao {
//
//	private static final long serialVersionUID = 1L;
//
//	public static final int UPDATE=0;
//	public static final int INSERT=1;
//	public static final int DELETE=2;
//	private JSONObject parameters=null;
//	private String table="";
//	private int sqlType =0;
//	private String idField="";
//	public DbCommand(Connection connection) {
//		super(connection);
//	}
//
//	public void setSql(int sqlType,String table,String w){
//		parameters=new JSONObject();
//		this.table=table;
//		this.sqlType =sqlType;
//		this.idField=","+w+",";
//	}
//	public void setSql(int sqlType,String table){
//		parameters=new JSONObject();
//		this.table=table;
//		this.sqlType =sqlType;
//		this.idField=","+idField+",";
//	}
//
//	public JSONObject parameters(){
//		return parameters;
//	}
//}