package com.topfox.spring;

import com.topfox.common.CommonException;
import com.topfox.common.ResponseCode;
import com.topfox.conf.CustomRedisTemplate;
import com.topfox.misc.DateUtils;
import com.topfox.misc.Misc;
import com.topfox.sql.Condition;
import com.topfox.data.DataHelper;
import com.topfox.data.TableInfo;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 拼接流水号 = 前缀+日期字符+递增序号+后缀
 */
public class KeyBuild {
    private SimpleService baseService;
    private CustomRedisTemplate redisTemplate;
    private ValueOperations redisVO;

    private TableInfo tableInfo;
    private String prefix="";    //前缀
    private String dateFormat="";//yyyyMMdd yyMMdd yyyyMM yyMM等
    private String suffix="";    //后缀

    private String prefixDateString="";//前缀+日期字符串.  日期字符串:(例2018年10月1日)自动生成, 例20181001  181001  201810 1010
    StringBuilder stringBuilder;
    public KeyBuild(CustomRedisTemplate redisTemplate, SimpleService baseService){
        this.redisTemplate = redisTemplate;
        //redisTemplate.expire("KeyBuild", 24, TimeUnit.HOURS);//设置超时时间24小时 第三个参数控制时间单位，详情查看TimeUnit
        this.redisVO =redisTemplate.opsForValue();

        this.baseService=baseService;
        this.baseService=baseService;
        if (baseService.clazz==null) {
            baseService.beforeInit(null);
        }
        tableInfo=TableInfo.get(baseService.clazz);
        this.stringBuilder=new StringBuilder();
    }

    /**
     * 设置流水号的前缀
     * @param prefix
     */
    public KeyBuild setPrefix(String prefix){
        this.prefix=prefix;
        return this;
    }

    /**
     * 设置流水号的后缀
     * @param suffix
     */
    public KeyBuild setSuffix(String suffix){
        this.suffix=suffix;
        return this;
    }

    /**
     * 设置流水号的日期格式
     * @param dateFormat yyyyMMdd yyMMdd yyMM
     * @return
     */
    public KeyBuild setDateFormat(String dateFormat){
        this.dateFormat=dateFormat;
        return this;
    }

    /**
     * 主方法,获得一个流水号
     * @param fieldName  字段名, 必须是DTO中存在的
     * @param fillLength 流水号的长度, 不足用0填充
     * @return
     */
    public String getKey(String fieldName, int fillLength){
        int maxNumber = getMaxNumber(fieldName, fillLength);
        String maxIdString= Misc.fillStr(maxNumber, fillLength,"0");//不足指定位数,则用0填充

        redisVO.set(redisHashKey, String.valueOf(maxNumber), 24,TimeUnit.HOURS);////保存到Redis, 避免下次查询数据库
        return megerKey(maxIdString);
    }


    /**
     * 主方法,获得多个个流水号
     * @param fieldName  字段名, 必须是DTO中存在的
     * @param fillLength 流水号的长度, 不足用0填充
     * @param count      一次获取的流水号个数
     *
     * @return 返回 队列对象 方法 poll() 获取并删除一个,先进先出的原则
     */
    public ConcurrentLinkedQueue getKeys(String fieldName, int fillLength, int count){
        ConcurrentLinkedQueue concurrentLinkedQueue = new ConcurrentLinkedQueue();
        int maxNumber = getMaxNumber(fieldName, fillLength);
        while(count>0){
            String maxIdString= Misc.fillStr(maxNumber,fillLength,"0");//不足指定位数,则用0填充
            concurrentLinkedQueue.add(megerKey(maxIdString));
            getKey(fieldName, fillLength);
            count--;
            if (count >0 ) {
                maxNumber++;
            }
        }
        redisVO.set(redisHashKey, String.valueOf(maxNumber), 24,TimeUnit.HOURS);////保存到Redis, 避免下次查询数据库
        return concurrentLinkedQueue;//concurrentLinkedQueue.poll();//获取并删除一个
    }

    private String redisHashKey;//存在Redis的key
    /**
     * 获得最大的序号,在上次值的基础上 + 1
     * @param fieldName
     * @return
     */
    private int getMaxNumber(String fieldName, int fillLength){
        if (tableInfo.getFields().get(fieldName)==null){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_INEXISTENCE).text("字段"+fieldName+"在实体DTO中不存在");
        }
        //prefixDateString=前缀+日期字符创
        prefixDateString=prefix;
        if (dateFormat.length()>0) { //设置过日期格式,  则生成日期字符串
            prefixDateString += DateUtils.getDate2String(dateFormat);
        }

        //构造存在Redis的key
        StringBuilder sbKey = new StringBuilder();
        sbKey.append("KeyBuild:")
                .append(baseService.clazz.getName())        //DTO的类名称
                .append("-").append(fieldName.toUpperCase());//字段名,都转大写
        if (Misc.isNotNull(prefixDateString)){
            sbKey.append("-").append(prefixDateString).toString(); //前缀+日期字符
        }
        redisHashKey=sbKey.toString();

        Object maxNumber = redisVO.get(redisHashKey);//从Redis获取上次使用过的序列号
        if (maxNumber==null){
            //如果redis获取不到,就从数据库中查询一次
            Condition where = baseService.where();
            if (Misc.isNull(prefixDateString)==false && Misc.isNull(suffix)==false || Misc.isNull(suffix)==false) {
                where.like(fieldName,prefixDateString + "%" + suffix);
            }else if (Misc.isNull(prefixDateString)==false){
                where.add("LOCATE('"+prefixDateString+"',"+fieldName+") = 1");
            }else{
                //前缀,日期和后缀 都没有的情况
                where.add("length("+fieldName+")="+fillLength);
            }
            maxNumber=baseService.selectMax(fieldName, where);//Redis没有, 则执行Sql获得最大Id
            if (!prefixDateString.isEmpty()) {
                maxNumber=maxNumber.toString().replace(prefixDateString,"");
            }
        }

        int max= DataHelper.parseInt(maxNumber)+1;


        return max;
    }

    /**
     * 拼接流水号 前缀+日期字符+递增序号+后缀
     * @param number
     * @return
     */
    private String megerKey(String number){
        stringBuilder.setLength(0);
        if (!Misc.isNull(prefixDateString)) {    //前缀+日期字符串
            stringBuilder.append(prefixDateString);
        }
        stringBuilder.append(number);  //递增序号
        if (!Misc.isNull(suffix)) {    //后缀
            stringBuilder.append(suffix);
        }
       return stringBuilder.toString();
    }



    public  static void main(String[] args) throws IOException
    {
        for (int i=0;i<=2100;i++){
            System.out.println(getKeyID());
        }
    }

    //线程安全的 AtomicInteger
    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 纯算法生成全局唯一 流水号
     * @return
     */
    public static String getKeyID(){
        atomicInteger.getAndIncrement();
        if(atomicInteger.get()>999)atomicInteger.set(1);
        String keyID= DateUtils.getDateByHHmmssSSS().replaceAll("-","").replaceAll(" ","").replaceAll(":","");
        keyID=keyID.substring(2,keyID.length())+"R"
                +Misc.fillStr(String.valueOf((int)(Math.random()*999)), 3, "0")
                +"S"+Misc.fillStr(atomicInteger.get()+"", 3, "0");
        try { Thread.sleep(1); } catch (Exception e) {}
//        String keyID=Misc.getDate().toString().replaceAll("-","").substring(2)+System.nanoTime()+""+Misc.fillStr(count+"", 3, "0");//getKeyID();
        if(keyID.length()>26) {
            return keyID.substring(0, 26);
        }else {
            return keyID;
        }
    }
}
