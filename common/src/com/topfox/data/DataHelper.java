package com.topfox.data;

import com.topfox.misc.DateFormatter;
import com.topfox.misc.DateUtils;
import com.topfox.misc.Misc;

import java.math.BigDecimal;
import java.util.Date;

public final class DataHelper {
	public static Object toObject(byte value) {
		return new Byte(value);
	}

	public static Object toObject(short value) {
		return new Short(value);
	}

	public static Object toObject(int value) {
		return new Integer(value);
	}

	public static Object toObject(long value) {
		return new Long(value);
	}

	public static Object toObject(float value) {
		return new Float(value);
	}

	public static Object toObject(double value) {
		return new Double(value);
	}

	public static Object toObject(boolean value) {
		return new Boolean(value);
	}

	public static Object toObject(Date value) {
		return value;
	}

	public static Object toObject(BigDecimal value) {
		return value;
	}

	public static Object toObject(String value) {
		return value;
	}

	public static String parseString(Object o) {
		if (o == null || o.toString().equals("undefined") || o.toString().equals("null")) {
			return "";
		}
		if (o instanceof Date) {
			return String.valueOf(((Date) o).getTime());
		}
		String stringValue = o.toString();
		stringValue=stringValue.trim();
		return stringValue;
	}

	/**
	 * 处理 英文单引号 保存 查询的问题
	 * @param o
	 * @return
	 */
	public static String parseString2(Object o) {
		String stringValue = parseString(o);
		if(stringValue.indexOf("\\")>=0) stringValue=stringValue.replace("\\","\\\\");
		if(stringValue.indexOf("'")>=0) stringValue=stringValue.replace("'","\\'");
		return stringValue;
	}


	public static byte parseByte(Object o) {
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).byteValue();
		if (o instanceof Boolean) {
			return (byte) ((((Boolean) o).booleanValue()) ? 1 : 0);
		}
		String s = o.toString();
		if (s.equals("") || s.equals("NaN")) {
			return 0;
		}
		return Byte.parseByte(s);
	}

	public static short parseShort(Object o) {
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).shortValue();
		if (o instanceof Boolean) {
			return (short) ((((Boolean) o).booleanValue()) ? 1 : 0);
		}
		String s = o.toString().trim();
		if (s.equals("") || s.equals("NaN")) {
			return 0;
		}
		return Short.parseShort(s);
	}

	public static int parseInt(Object o) {
		if (o == null){
			return 0;
		}
		if (o.toString().indexOf("'")>=0){
			throw new RuntimeException(o.toString()+" 不能转换为整型");
		}

		if (o instanceof Number)
			return ((Number) o).intValue();
		if (o instanceof Boolean) {
			return ((((Boolean) o).booleanValue()) ? 1 : 0);
		}
		String s = o.toString().replace(",","").replace("'","").trim();
		if (s.equals("") || s.equals("NaN")) {
			return 0;
		}

		if (s.length()>9){
			throw new RuntimeException("内容过长，超出设计限制");
		}
		return Integer.parseInt(s);
	}

	public static long parseLong(Object o) {
		if (o == null)
			return 0L;
		if (o instanceof Number)
			return ((Number) o).longValue();
		if (o instanceof Boolean)
			return ((((Boolean) o).booleanValue()) ? 1L : 0L);
		if (o instanceof Date) {
			return ((Date) o).getTime();
		}
		String s = o.toString().replace(",","").trim();
		if (s.equals("") || s.equals("NaN")) {
			return 0L;
		}
		if (s.length()>18){
			throw new RuntimeException(" 内容过长，超出设计限制");
		}
		return Long.parseLong(s);
	}

	public static float parseFloat(Object o) {
		if (o == null)
			return 0.0F;
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof Boolean) {
			return ((((Boolean) o).booleanValue()) ? 1.0F : 0.0F);
		}
		String s = o.toString().trim();
		if (s.equals("")) {
			return 0.0F;
		}
		return Float.parseFloat(s);
	}

	public static double parseDouble(Object o) {
		if (o == null)
			return 0.0D;
		if (o instanceof Number)
			return ((Number) o).doubleValue();
		if (o instanceof Boolean) {
			return ((((Boolean) o).booleanValue()) ? 1.0D : 0.0D);
		}
		String s = o.toString().replace(",","").trim();
		if (s.equals("") || s.equals("NaN")) {
			return 0.0D;
		}
		return Double.parseDouble(s);
	}

	public static BigDecimal parseBigDecimal(Object o) {
		if (o == null)
			return new BigDecimal(0);
		if (o instanceof BigDecimal)
			return ((BigDecimal) o);
		if (o instanceof Number)
			return new BigDecimal(((Number) o).doubleValue());
		if (o instanceof Boolean) {
			return new BigDecimal((((Boolean) o).booleanValue()) ? 1 : 0);
		}
		String s = o.toString().replace(",","").trim();
		if (s.equals("") || s.equals("NaN")) {
			return new BigDecimal(0);
		}
		return new BigDecimal(s);
	}

	public static boolean parseBoolean(String s) {
		if (s == null) {
			return false;
		}
		return ((s.equalsIgnoreCase("true")) || (s.equals("1")) || (s.equals("-1")) || (s.equalsIgnoreCase("T"))
				|| (s.equalsIgnoreCase("Y")));
	}

	public static boolean parseBoolean(Object o) {
		if (o == null)
			return false;
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue();
		}
		return parseBoolean(o.toString());
	}

	private static boolean isNumeric(String s) {
		int length = s.length();
		for (int i = 0; i < length; ++i) {
			char c = s.charAt(i);
			if ((((c < '0') || (c > '9'))) && (c != '.') && (((i != 0) || (c != '-')))) {
				return false;
			}
		}

		return true;
	}

	public static Date parseDate(Object o) {
		if (o == null)	return null;
		if ((o instanceof String) && (((String)o).length() == 0)) return null;

		if (o instanceof Date)return ((Date) o);
		if (o instanceof Number) {
			return new Date(((Number) o).longValue());
		}
		String stringValue = String.valueOf(o).trim();
		if (Misc.isNull(stringValue)==false) {
			if (isNumeric(stringValue)) {
				long time = Long.parseLong(stringValue);
				return new Date(time);
			}else if(stringValue!=null&&stringValue.indexOf("T")>0){
				//处理javascript默认格式字符 yyyy-MM-dd'T'HH:mm:ss'Z'
				try{
					String stringValue2=stringValue.replaceAll("T"," ");
					stringValue2=stringValue2.substring(0,stringValue2.indexOf("."));
					Date d= DateUtils.stringToDate(stringValue2);
					return DateUtils.getChangeDate(d, "hour", 8);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}

			int len = stringValue.length();
			stringValue=stringValue.replace("年", "-");
			stringValue=stringValue.replace("月", "-");
			stringValue=stringValue.replace("日", "-");
			stringValue=stringValue.replace("/",  "-");
			if (stringValue.indexOf(":") > 0) {
				if (len == 16) {
					return DateUtils.toDate(stringValue, DateFormatter.DATETIME_FORMAT);          //"yyyy-MM-dd HH:mm"
				}else if (len == 17||len == 18||len == 19) {
					return DateUtils.toDate(stringValue, DateFormatter.DATETIME_SECOND_FORMAT);   //yyyy-MM-dd HH:mm:ss
				}else if (len == 23) {
					return DateUtils.toDate(stringValue, DateFormatter.DATETIME_MILLSECOND_FORMAT);//yyyy-MM-dd HH:mm:ss SSS
				}else if (len == 8) {
					return DateUtils.toDate(stringValue, DateFormatter.TIME_SECOND_FORMAT); //HH:mm:ss
				}else{//len == 5
					return DateUtils.toDate(stringValue, DateFormatter.TIME_FORMAT); //HH:mm
				}
			}else{
				return DateUtils.toDate(stringValue, DateFormatter.DATE_FORMAT);
			}
		}

		return null;
	}


	/***
	* 判断 String 是否是 int
	*
	* @param input
	* @return
	*/
	public static boolean isInteger(String input){
		return input.matches("[-+]?[0-9]*");
	   }


//	public static Object translate(int dataType, Object value) {
//		if ((value == null) || ((value instanceof String) && (((String)value).length() == 0))) {
//			if (dataType == 1) {
//				return value;
//			}
//			return null;
//		}
//
//		switch (dataType) {
//		case 1:
//			return parseString(value);
//		case 9:
//			return toObject(parseBoolean(value));
//		case 10:
//		case 11:
//		case 12:
//			return parseDate(value);
//		case 4:
//			return toObject(parseInt(value));
//		case 7:
//			return toObject(parseDouble(value));
//		case 5:
//			return toObject(parseLong(value));
//		case 6:
//			return toObject(parseFloat(value));
//		case 8:
//			return parseBigDecimal(value);
//		case 2:
//			return toObject(parseByte(value));
//		case 3:
//			return toObject(parseShort(value));
//		}
//
//		return value;
//	}



	public static double round(double number,String format){
		java.text.DecimalFormat   df=new   java.text.DecimalFormat(format);
		String amt=df.format(number);
		return Double.parseDouble(amt);
	}
	public static double round(double dSource, int weishu){
		return round(dSource,weishu,BigDecimal.ROUND_HALF_UP);
	}

	public static String roundToString(double number,String format){
		format = Misc.isNull(format)?"###0.00":format;//没有格式化信息  则默认 精确到两位小数
		java.text.DecimalFormat   df=new   java.text.DecimalFormat(format);
		String amt=df.format(number);
		return amt;
	}
	public static String roundToString(Object number,String format){
		return roundToString(parseDouble(number), format);
	}
	public static String roundWeight2String(double weight){
		return roundToString(weight,"###0.000");
	}

	public static String roundCubage2String(double cubage){
		return roundToString(cubage,"###0.0000");
	}

	/**
	 * 对double数据进行取精度.
	 * @param value  double数据.
	 * @param scale  精度位数(保留的小数位数).
	 * @param roundingMode  精度取值方式.
	 *        四舍五入：BigDecimal.ROUND_HALF_UP
	 *        截取               BigDecimal.ROUND_DOWN
	 * @return double 精度计算后的数据.
	 */
	public static double round(double value, int scale,
							   int roundingMode) {
		double value2=value;
		if (value<0)value2=-value;
		BigDecimal bd = new BigDecimal(Double.toString(value2));
		bd = bd.setScale(scale, roundingMode);
		double ret = bd.doubleValue();
		bd = null;
		return value<0?-ret:ret ;
	}
}