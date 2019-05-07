package com.topfox.spring;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.topfox.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// P6SpyLogger
public class P6SpyLogger implements MessageFormattingStrategy {
    protected Logger logger= LoggerFactory.getLogger(getClass());

    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                String category, String prepared, String sql) {
//        if(sql!=null && sql.length()>0 && logger.isDebugEnabled()) {
//            // 去掉多个换行
//            Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
//            Matcher m = p.matcher(sql);
//            sql = m.replaceAll("\r\n");
//
////            logger.debug("SQL格式化:");
////            logger.debug("\n"+sql);
////            logger.debug(format(sql));//没有换行符
//        }
        return sql;
    }


    public synchronized static String format2(String str){
        str=str.replaceAll("\n\t", "|||").replaceAll("[\\s]{2,10}", " ")
                .replaceAll("\\|\\|\\|\\s", "|||");
        while(str.indexOf("||||||")!=-1){
            str=str.replaceAll("\\|\\|\\|\\|\\|\\|", "|||");
        }
        return str.replaceAll("\\|\\|\\|", "\n\t");
    }

    //sql语句格式化
    public synchronized static String format(String str) {
        if(Misc.isNull(str))return "";
        str=str.trim()+" -endsql";
        str=str.replaceAll("\n", " ");
        str=str.replaceAll("[\\s]{2,10}", " ");
        str=str.replaceAll("\\,\\s", ",");
        str=str.replaceAll("\\s=\\s", "=");
        str=splitFor_dh(str);
        str=splitFor_or_and(str);
        str=replace(str);
        return "\n "+str;
    }
    public synchronized static String replace(String str){
        str=replaceAll(str,"\\sas\\s[a-z|0-9]+[_]+[0-9]+[_]+[0-9]+[_]", "",false);
        str=replaceAll(str,"\\sas\\s[a-z|0-9]+[_]+[0-9]+[_]", "",false);
        str=replaceAll(str,"[\\s][a-z|0-9]+[_]+[.]", " ",false);
        str=replaceAll(str,"[,][a-z|0-9]+[_]+[.]",", ",false);
        str=replaceAll(str,"\\s[a-z|0-9]+[_]\\s", " ",false);
        str=replaceAll(str,"\\sset\\s", "\n set\n \t",false);
        str=replaceAll(str,"\\sinto\\s", " into\n   ",false);
        str=replaceAll(str,"\\sleft\\s", "\n left ",false);
        str=replaceAll(str,"\\sinner\\s", "\n inner ",false);
        str=replaceAll(str,"\\sright\\s", "\n right ",false);
//		str=replaceAll(str,"\\sand\\s", " and\n ",false);
        //	str=replaceAll(str,"\\sor\\s", " or\n ",false);
        str=replaceAll(str,"\\swhere\\s", "\n where\n \t",false);
        str=replaceAll(str,"\\sgroup\\sby\\s", "\n group by\n \t",false);
        str=replaceAll(str,"\\sorder\\sby\\s", "\n order by\n \t",false);
        str=replaceAll(str,"select\\s", " select\n \t",false);
        //str=replaceAll(str,"\\sselect\\s", "\n select \n\t",false);
        str=replaceAll(str,"insert\\s", " insert ",false);
        str=replaceAll(str,"\\svalues\\s", "\n values\n \t",false);
        str=replaceAll(str,"update\\s", "update\n\t ",false);
        str=replaceAll(str,"delete\\s", "delete ",false);
        str=replaceAll(str,"\\sfrom\\s", "\n from\n \t",false);
        str=replaceAll(str,"\\slimit\\s", "\n limit ",false);
        return str;
    }
    public synchronized static String replace_kh(String str){
        str=str.replaceAll("\\,", ",\n \t");
        return str;
    }
    public synchronized static String replace_and_or(String str){
        str=str.replaceAll(" and ", " and\n \t");
        str=str.replaceAll(" or ", " or\n \t");
        return str;
    }
    //小括号里面内容不换行
    public synchronized static String split_and_or(String str) {
        if(Misc.isNull(str))return "";
        StringBuffer sb=new StringBuffer();
        if(str.indexOf("(")==-1){
            str=replace_and_or(str);
            return str;
        }
        String[] sql=str.split("\\(");
        int count=0;
        for(int i=0;i<sql.length;i++){
            String s=sql[i];
            if(i==0){
                count=1;
                s=replace_and_or(s);
                sb.append(s);
            }else{
                int idx0=s.indexOf(")");
                if(idx0!=-1){
                    count+=-1;
                    String f=s.substring(0,s.indexOf(")")+1);
                    String f2=s.substring(s.indexOf(")")+1,s.length());
                    sb.append(f);
                    if(f2.indexOf(")")==-1){
                        if(count==0)sb.append(replace_and_or(f2));
                        else sb.append(f2);
                    }
                    while(f2.indexOf(")")!=-1){
                        count+=-1;
                        f=f2.substring(0,f2.indexOf(")")+1);
                        f2=f2.substring(f2.indexOf(")")+1,f2.length());
                        sb.append(f);
                        if(count==0)sb.append(replace_and_or(f2));
                        else sb.append(f2);
                    }
                }else{
                    sb.append(s);
                }

                count+=1;
            }
            if(i!=sql.length-1)sb.append("(");
        }
        return sb.toString();
    }
    //小括号里面内容不换行
    public synchronized static String split_kh(String str) {
        if(Misc.isNull(str))return "";
        StringBuffer sb=new StringBuffer();
        if(str.indexOf("(")==-1){
            str=replace_kh(str);
            return str;
        }
        String[] sql=str.split("\\(");
        int count=0;
        for(int i=0;i<sql.length;i++){
            String s=sql[i];
            if(i==0){
                count=1;
                s=replace_kh(s);
                sb.append(s);
            }else{
                int idx0=s.indexOf(")");
                if(idx0!=-1){
                    count+=-1;
                    String f=s.substring(0,s.indexOf(")")+1);
                    String f2=s.substring(s.indexOf(")")+1,s.length());
                    sb.append(f);
                    while(f2.indexOf(")")!=-1){
                        count+=-1;
                        f=f2.substring(0,f2.indexOf(")")+1);
                        f2=f2.substring(f2.indexOf(")")+1,f2.length());
                        sb.append(f);
                        if(count==0)sb.append(replace_kh(f2));
                        else if(count!=0&&f2.indexOf(")")==-1) sb.append(f2);
                    }
                }else{
                    sb.append(s);
                }

                count+=1;
            }
            if(i!=sql.length-1)sb.append("(");
        }
        return sb.toString();
    }
    //单引号里面的内容不换行
    public synchronized static String splitFordyh(String str){
        StringBuffer sb=new StringBuffer();
        String[] sql=str.split("='");
        for(int i=0;i<sql.length;i++){
            String s=sql[i];
            if(i==0){
                s=s.replaceAll("\\,", ",\n ");
                sb.append(s);
                if(sql.length!=1)sb.append("='");
            }else{
                String f=s.substring(0,s.indexOf("'")+1);
                String f2=s.substring(s.indexOf("'")+1,s.length());
                sb.append(f);
                f2=f2.replaceAll("\\,", ",\n ");
                sb.append(f2);
                if(i!=sql.length-1)sb.append("='");
            }
        }
        return sb.toString();
    }
    public synchronized static String replaceAll(String str,String str1,String str2,boolean isT){
        Pattern pat = null;
        //	if(!isT)pat=Pattern.compile(str1,Pattern.CASE_INSENSITIVE);
        //else
        pat=Pattern.compile(str1);
        Matcher m = pat.matcher(str);
        str=m.replaceAll(str2);
        return str;
    }
    public synchronized static String splitFor_or_and(String sql){
        sql=replaceAll(sql, "where", "where", false);
        sql=replaceAll(sql, "order", "order", false);
        sql=replaceAll(sql, "group", "group", false);
        sql=replaceAll(sql, "and", "and", false);
        sql=replaceAll(sql, "or", "or", false);
        if(sql.indexOf("where")!=-1||sql.indexOf(")")==-1){
            String nstr="";
            String slt_sql=sql;
            while(slt_sql.indexOf("where")!=-1){
                int dex1=slt_sql.indexOf("where")+5;
                String str=slt_sql.substring(0,dex1);
                String str2=slt_sql.substring(dex1,slt_sql.length());
                int dex2=str.length()+getLastKH(str2);
                String s1=slt_sql.substring(dex1,dex2);
                slt_sql=slt_sql.substring(dex2,slt_sql.length());
                nstr+=(!Misc.isNull(str)?str+"\n":"")+split_and_or(s1);
            }
            if(!Misc.isNull(slt_sql))nstr+=slt_sql;
            sql=nstr;
        }
        return sql;
    }
    public synchronized static int getLastKH(String str){
        int len=str.length();
        if(str.indexOf(")")>0){
            int left_kh=0,right_kh=0;
            for(int x=0;x<str.length();x++){
                char zfc=str.charAt(x);
                if(zfc=='(')left_kh+=1;
                else if(zfc==')'){
                    right_kh+=1;
                    if(right_kh>left_kh){
                        len=x+1;
                        break;
                    }
                }
            }
        }
        return len;
    }
    //逗号换行
    public synchronized static String splitFor_dh(String sql){
        Pattern pat = null;
        pat=Pattern.compile("(insert into \\w).*(values)",Pattern.CASE_INSENSITIVE);
        Matcher m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String valueStr= s.replaceAll("\\,", ",\n ");
            sql=sql.replace(s,valueStr);
        }
        pat=Pattern.compile("(insert).*(select)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            sql=sql.replace(s, s.replaceAll("\\,", ",\n "));
        }
        sql=replaceAll(sql, "select", "select", false);
        sql=replaceAll(sql, "from", "from", false);
        if(sql.indexOf("select")!=-1){
            String nstr="";
            String slt_sql=sql;
            while(slt_sql.indexOf("select")!=-1){
                int dex1=slt_sql.indexOf("select");
                String str=slt_sql.substring(0,dex1);
                String str2=slt_sql.substring(dex1,slt_sql.length());
                int dex2=str.length()+str2.indexOf("from")+4;
                String s1=slt_sql.substring(dex1,dex2);
                String s2=slt_sql.substring(0,dex1);
                slt_sql=slt_sql.substring(dex2,slt_sql.length());
                nstr+=(!Misc.isNull(s2)?s2+"\n":"")+split_kh(s1);
            }
            if(!Misc.isNull(slt_sql))nstr+=slt_sql;
            sql=nstr;
        }
        pat=Pattern.compile("(set).*(from)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String fg=split_kh(s);
            sql=sql.replace(s, fg);
        }

        pat=Pattern.compile("(group by ).*(-endsql)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String fg=split_kh(s);
            sql=sql.replace(s, fg);
        }
        pat=Pattern.compile("(order by ).*(-endsql)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String fg=split_kh(s);
            sql=sql.replace(s, fg);
        }
        pat=Pattern.compile("(update .* set).*(where)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String fg=splitFordyh(s);
            sql=sql.replace(s, fg);
        }
        pat=Pattern.compile("values\\(.*\\)",Pattern.CASE_INSENSITIVE);
        m = pat.matcher(sql);
        if(m.find()){
            String s=m.group();
            String ns="";
            String[] arr_str=s.split("\\'");
            for(int x=0;x<arr_str.length;x++){
                String a_str=arr_str[x];
                if(x%2==0){
                    ns+=a_str.replaceAll("\\,", ",\n ");
                }else ns+=a_str;
                if(x!=arr_str.length-1)ns+="'";
            }
            sql=sql.replace(s, ns);
        }
        sql=sql.replace(" -endsql", "");
        return sql;
    }
}