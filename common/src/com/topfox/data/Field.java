package com.topfox.data;

import com.topfox.annotation.TableField;
import com.topfox.common.Incremental;
import com.topfox.common.SysConfigRead;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;

public class Field {
	/**
	 * DTO QTO VO BO 中定义的Field成员变量(字段)名
	 */
	private String name;

	/**
	 * 数据库中的字段名 来源TableField的注解或者 ...
	 */
	private String dbName;

//	/**
//	 * 前台传入的字段名,  可能和数据库字段名 以及 [DTO QTO VO BO 中定义的Field成员变量(字段)名]定义的不一致
//	 */
//	private String underscoreName;

	/**
	 * 格式化字符串,  主要是日期的格式化  小数位数的格式化
	 */
	private String format;


	private String leftName=null;
	private Incremental incremental; //生成更新SQL 加减  值:addition +  / subtract -
	private DataType dataType;
	private boolean fillUpdate=false;
	private boolean fillInsert=false;

	/**
	 * 是否使用了注解字段名,  这种字段类库将不生成查询SQL
	 */
	private boolean annotationName=false;

	public Field(TableField tableField, String _name, DataType _dataType, SqlUnderscore sqlUnderscore){
//		sysConfigRead.getSqlCamelToUnderscore()的3个值
//		*  1. OFF 关闭
//		*  2. ON-UPPER 打开并大写
//		*  3. ON-LOWER 打开并小写

		// 默认使用驼峰名字
		String _dbName;
		if (tableField !=null && Misc.isNotNull(tableField.name())){
			//使用注解的字段名
			_dbName = tableField.name();
			annotationName=true;//是否使用了注解字段名,  这种字段类库将不生成查询SQL
		}else if (sqlUnderscore == SqlUnderscore.ON_UPPER){
			//开启 字段名依据驼峰命名转下划线, 并全部 大写
			_dbName = BeanUtil.toUnderlineName(_name).toUpperCase();
		}else if (sqlUnderscore == SqlUnderscore.ON_LOWER){
			//开启 字段名依据驼峰命名转下划线, 并全部 小写
			_dbName = BeanUtil.toUnderlineName(_name).toLowerCase();
		}else{
			//使用驼峰名字
			_dbName = _name;
		}
		this.name    = _name;
		this.dbName  = _dbName;
		this.dataType= _dataType;
	}

//	public Field(String name,String dbName, DataType datatype){
//		setName(name);
//		setDbName((Misc.isNull(dbName)?BeanUtil.toUnderlineName(name).toLowerCase():dbName));
//		setUnderscoreName(BeanUtil.toUnderlineName(name).toLowerCase());
//		setDataType(datatype);
//	}
//	public Field(String name,DataType datatype){
//		this(name, name, datatype);
//	}

	public Boolean getAnnotationName(){
		return annotationName;
	}
	public String getName(){
		return name;
	}
//	private void setName(String value){
//		name=value.trim();
//	}

	public String getDbName(){
		return dbName;
	}
//	private void setDbName(String value){
//		dbName=value.trim();
//	}

//	/**
//	 * 驼峰命名的变量转 带下划线的
//	 * @return
//	 */
//	public String getUnderscoreName(){
//		return underscoreName;
//	}
//	private void setUnderscoreName(String value){
//		underscoreName=value.trim();
//	}

	public String getFormat(){
		return format;
	}
	public void setFormat(String format){
		this.format = format==null?"":format.trim();
	}

	public String getLabel(){
//		return FieldInfo.getString(getName());
		return name;
	}
	
	/**
	 * 例如：字段名是tohOrgId则返回 toh
	 */
	public String getLeftName(){
		if (leftName==null)
			leftName=getFieldLeft(getName());
		return leftName;
	}
	/**
	 * 例如：字段名是tohOrgId则返回 orgId,注意第一个字母是小写
	 */
	public String getRightName1(){
		String sname=name.substring(getLeftName().length());
		String first=sname.substring(0,1);
		return first.toLowerCase()+sname.substring(1);
	}
	/**
	 * 例如：字段名是tohOrgId则返回 OrgId,注意第一个字母是大写
	 */
	public String getRightName2(){
//		int pos =getLeftName().length();
//		if (pos>0){
//			return _name.substring(getLeftName().length());
//
//		}else{
//			return Character.toUpperCase(_name.charAt(0))+_name.substring(1);
//		}
		return Character.toUpperCase(name.charAt(0))+name.substring(1);

	}
	
	public DataType getDataType(){
		return dataType;
	}

	private void setDataType(DataType value){
		if (Misc.isNull(value)){
			throw new RuntimeException(name+" 的类型不能为空");
		}
		dataType=value;
	}

	private  static String getFieldLeft(String columnName) {
		columnName=columnName.trim();
		String tableSName="";
		if (columnName.indexOf("_")<0){
			for (int i = 0; i < columnName.length(); i++) {
				char c = columnName.charAt(i);
				if (Character.isUpperCase(c)) {
					tableSName=columnName.substring(0, i);
					break;
				}
			}
		}else{
			 tableSName=columnName.substring(0,columnName.indexOf("_"));
		}
		return tableSName;
	}

	/**
	 * 是否更新填充字段
	 * @return
	 */
	public boolean isFillUpdate() {
		return fillUpdate;
	}

	public void setFillUpdate(boolean fillUpdate) {
		this.fillUpdate = fillUpdate;
	}

	/**
	 * 生成SQL + -
	 * @return
	 */
	public Incremental getIncremental() {
		return incremental;
	}
	public void setIncremental(Incremental incremental) {
		this.incremental = incremental;
	}

	/**
	 * 是否插入填充字段
	 * @return
	 */
	public boolean isFillInsert() {
		return fillInsert;
	}

	public void setFillInsert(boolean fillInsert) {
		this.fillInsert = fillInsert;
	}


	@Override
	public String toString(){
		StringBuilder temp=new StringBuilder();
		temp.append(getClass().getName()).append("[name:").append(getName())
				.append(", dataType:").append(getDataType().getValue())
				.append(", fillUpdate:").append(isFillUpdate())
				.append(", fillInsert:").append(isFillInsert()).append("]");
		return temp.toString();
	}
}