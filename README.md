[TOC]目录

# topfox 介绍

* 无侵入, 轻损耗小
* 强大的 CRUD 操作：内置通用Service，仅仅通过少量配置即可实现单表大部分CRUD操作，更有强大的条件构造器，满足各类使用需求
* CRUD方法命名规范使用alibaba操作手册1.4规范
* 支持 自增 式更新库存(支持多主键), 仅需entity注解, 字段使用隔离性大
* 内置全局、局部拦截插件：提供delete 、 update 自定义拦截功能
* Redis：自动缓存功能, 支持多主键
* 自带分页功能

pom.xml: 

```
<!--topfox-->
<dependency>
    <groupId>com.topfox</groupId>
    <artifactId>topfox</artifactId>
    <version>1.0.7</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

## 配置模板相关
* top.log.start="#########################################"
> debug开头日志输出分割
* top.log.prefix=" "
> debug中间日志输出前缀
* top.log.end="#########################################"
> debug结尾日志输出分割
* top.page-size=100
> 分页时,默认的每页条数
* top.max-page-size=300
> 不分页时(pageSize<=0),查询时最多返回的条数
* top.service.open-redis=false
> service层是否开启redis缓存

* top.redis.serializer-json=true
```
# redis序列化支持两种, true:jackson2JsonRedisSerializer false:JdkSerializationRedisSerializer
# 注意, 推荐生产环境下更改为 false, 类库将采用JdkSerializationRedisSerializer 序列化对象,
# 这时必须禁用devtools(pom.xml 注释掉devtools), 否则报错.
```
* top.service.update-mode=1
```
# 重要参数:更新时DTO序列化策略 和 更新SQL生成策略
# 1 时, service的DTO=提交的数据.               更新SQL 提交数据不等null 的字段 生成 set field=value
# 2 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据) 的字段 生成 set field=value
# 3 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据 + 提交数据的所有字段)生成 set field=value
#   值为3, 则始终保证了前台(调用方)提交的字段, 不管有没有修改, 都能生成更新SQL, 这是与2最本质的区别
```
* top.service.sql-camel-to-underscore
生成SQL 是否驼峰转下划线 默认 OFF

一共有3个值:
1. OFF 关闭, 生成SQL 用驼峰命名
2. ON-UPPER 打开, 下划线并全大写
3. ON-LOWER 打开, 下划线并全小写

### [进阶使用(跳转)](https://gitee.com/topfox/topfox/blob/dev/%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E.md)



# TopFox 所有自定义注解
源代码包路径 com.topfox.annotation
- @Table 实体类的注解, 属性如下:
    1. name 实体对应在数据库的表名
    2. redisKey 缓存在redis中的别名
- @Id 主键字段注解, 当字段名为id时可不注解,类库将自动识别
- @Version 版本号字段注解,用于实现乐观锁.当字段名为version时可不注解,类库自动识别.
- @TableField 字段注解
    1. exist 是否数据库存在的字段, 默认true
    2. name 数据库的字段名注解,生成插入更新的SQL语句的字段名用这个属性的值;此注解解析到 com.topfox.data.Field 对象的 DbName属性
    3. fillUpdate 是否为更新填充的字段, 默认false
    4. fillInsert 是否为插入填充的字段, 默认false
    5. incremental 生成更新SQL 加减  值:addition +  / subtract 默认为Incremental.NONE(枚举对象)
- @State 状态字段, 主要解决复杂的企业内部系统, 一次提交多条数据且同时存在增删改的情况时, 用于描述DTO的状态, 类库将根据这个状态值自动生成相对应的SQL(insert/update/delete)语句, 提交的数据状态为"无状态"时, 自动忽略.

状态字段值的枚举类源码如下(注意,不是枚举对象):
```java
package com.topfox.data;
public class DbState {
    public static final String INSERT="i"; //新增
    public static final String DELETE="d"; //删除
    public static final String UPDATE="u"; //更新
    public static final String NONE  ="n"; //无状态
}
```
# 实体对象的超类 DataDTO 
所有业务的实体对象继承该对象, 源码包 com.topfox.common.DataDTO
用户DTO实体的例子:
```java
//用户表实体
@Setter
@Getter
@Table(name = "SecUser")//数据库的表名注解
public class UserDTO extends DataDTO {
    @State //状态字段注解
    @TableField(exist = false) //状态字段在数据库是不存在的
    private transient String state = "n";
    
    @Id //主键字段的注解
    private String userId;
    
    @Version //版本号字段注解
    private Integer userVersion;
    
    private String name;
    private String password;
    
    @TableField(incremental = Incremental.ADDITION)//递增注解, 更新时始终在该字段更新前的值的基础上加上传入的值
    private Integer stockQty;//库存数量
    
}
```
# DataQTO
所有业务查询的对象继承该对象, 源码包 com.topfox.common.DataQTO

## 分页每页行数设置
- 将每页行数设置为小于等于0时,类库认为不分页, 将获取最大行数的参数生成查询SQL

```java
//java source:
xxxQTO.setPageSize(0); 
//或者 
xxxQTO.setPageSize(-1);
```

- 在配置文件 application.properties设置最大行数
 #不设置,这个参数默认20000的
top.max-page-size=5000

## QTO后缀增强查询
假定用户表有3个字段:
- 用户编码 id, 
- 用户姓名 name, 
- 年龄    age

我们新建一个UserQTO 源码如下:

```java 
@Setter
@Getter
@Table(name = "SecUser")
public class UserQTO extends DataQTO {
    private String id;            //用户id, 与数据字段名一样的
    private String age;           //年龄age,与数据字段名一样的
    
    private String name;          //用户姓名name, 与数据字段名一样的
    private String nameOrEq;      //用户姓名 后缀OrEq
    private String nameAndNe;     //用户姓名 后缀AndNe
    private String nameOrLike;    //用户姓名 后缀OrLike
    private String nameAndNotLike;//用户姓名 后缀AndNotLike
    
    private String age; //年龄

}
```

- 字段名 后缀OrEq
当 nameOrEq 写值为 "张三,李四" 时, 源码如下:

```java
@Service("userService_5")
public class C5_UserService extends SimpleService<UserQTO, UserDTO> {
    ...
    
    public void test1(){
        UserQTO userQTO = new UserQTO();
        userQTO.setNameOrEq("张三,李四");//这里赋值
        //依据QTO查询 listObjects会自动生成SQL, 不用配置 xxxMapper.xml
        List<UserDTO> listUsers = listObjects(userQTO);
        ...
    }
}
```
则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name = '张三' OR name = '李四')
```

- 字段名 后缀AndNe
当 nameAndNe 写值为 "张三,李四" 时, 则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name <> '张三' AND name <> '李四')
```
- 字段名 后缀OrLike
当 nameOrLike 写值为 "张三,李四" 时, 则将生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name LIKE CONCAT('%','张三','%') OR name LIKE CONCAT('%','李四','%'))
```

- 字段名 后缀AndNotLike
当 nameAndNotLike 写值为 "张三,李四" 时, 则生成SQL:

```sql
SELECT ...
FROM SecUser
WHERE (name NOT LIKE CONCAT('%','张三','%') AND name NOT LIKE CONCAT('%','李四','%'))     
```

以上例子是TopFox全自动生成的SQL
如果执行的是MyBaties xxxMapper.xml的SQL, 则条件部分配置如下:

```xml
<!-- 注意是 $ 而不是 # -->
<sql id="whereClause">
    <where>
         <if test = "nameOrEq != null">AND ${nameOrEq}</if>
         <if test = "nameAndNe != null">AND ${nameAndNe}</if>
         <if test = "nameOrLike != null">AND ${nameOrLike}</if>
         <if test = "nameAndNotLike != null">AND ${nameAndNotLike}</if>
    </where>
</sql>
<select id="list" resultMap="list">
    SELECT id, name, ...
    FROM SecUser a
    <include refid="whereClause"/>
    <if test="limit != null">${limit}</if>
</select>
<select id="listCount" resultType="int">
    SELECT count(*)
    FROM SecUser a
    <include refid="whereClause"/>
</select>
```
以上的配置, 类库将实现以下"后缀增强查询"功能

# com.topfox.spring.SimpleService
部分源码:
```java
public class SimpleService<QTO extends DataQTO, DTO extends DataDTO> {
    //系统可变参数对象, 修改的值只对当前Service有效, 继承的子类可以直接使用这个对象
    protected SysConfig sysConfig; 
    
    public Condition where() {
        return Condition.create(this.clazz);
    }
    
    ...
}


```
以下方法均在 SimpleService 类中已经实现, 直接使用即可.

## init()
初始化的方法, 在其他类(Service或者Controller)调用Service的方法前, 首先会执行本方法.
以用户表为例:
```java
@Service("userService")
public class UserService extends SimpleService<UserQTO, UserDTO> {
    @Autowired UserDao userDao;
    
    @Override
    protected void init(){
        super.init();
        //告诉类库用哪个dao层
        super.setBaseDao(xxxDao);
        
        //设置当前Service对应的DTO不启用Redis缓存. 不是必须写的, 这个参数默认为true
        sysConfig.setOpenRedis(false);
    }
}
```
## insert(DTO xxxDTO)
- 基本方法:  插入一条记录
- @param xxxDTO 实体对象
- @return 插入成功记录数

## insertList(List< DTO > list)
- 插入多条记录
- @param list 实体List对象
- @return 插入成功记录数

## insertGetKey(DTO xxxDTO)
- 当数据库的主键字段设置为自增时, 才调用这个方法插入.
- @return 返回数据库自增的Id值(注意,不是插入成功的记录数)

## update(DTO xxxDTO)
- 更新一条记录,如果版本号字段有值, 将加上乐观锁的SQL
- @param xxxDTO 实体对象
- @return 更新成功记录数

## updateList(List< DTO > list)
类库已经实现,直接使用即可. 没有特殊情况不要Override这个方法

## delete(DTO xxxDTO)
- 删除一条记录, 如果版本号字段有值, 将加上乐观锁的SQL
- @param xxxDTO 实体对象
- @return 删除成功记录数

## deleteByIds(Object... ids)
- 类库已经实现,直接使用即可. 根据传入主键Id值删除记录
- 主键值只支持字符串和整型
- 返回删除的记录数

## deleteList(List< DTO > list)
- 类库已经实现,直接使用即可. 根据list每个DTO的Id值删除记录.
- 返回删除的记录数

## beforeInsertOrUpdate(DTO xxxDTO, String state)
执行"insert/insertList/update/updateList/updateBatch"之前, 会先执行beforeInsertOrUpdate这个方法. 使用场景如复杂一点的校验逻辑写在这里.
例:
```java
@Service("userService")
public class UserService extends SimpleService<UserQTO, UserDTO> {
    @Autowired UserDao userDao;
    
     @Override
     /**
     * @param userDTO
     * @param state  DTO实体的状态, 分为新增i/修改u/删除d/无n 4种状态, 
     *     可参考 com.topfox.data.DbState
     */
    protected void beforeInsertOrUpdate(UserDTO userDTO, String state) {
        if(DbState.INSERT.equals(state)){
            /** 新增之前的业务逻辑 */
        }else if (DbState.UPDATE.equals(state)){
            /** 更新之前业务逻辑 */
        }
        /** 新增和更新之前的业务逻辑 */
    }
}
```

## afterInsertOrUpdate(DTO xxxDTO, String state)
执行插入/更新的SQL语句之后将调用afterInsertOrUpdate方法.

## beforeDelete(DTO xxxDTO)
执行"delete/deleteByIds/deleteBatch/deleteList()之前会先执行beforeDelete这个方法.
- @param xxxDTO 删除的实体对象
- @Return 无

## afterDelete(DTO xxxDTO)
执行删除的SQL语句之后将调用afterDelete方法.


#查询方法getObject/listObjects
> ##### 一二级缓存的概念
- 一级缓存是指当前线程中的缓存数据
- 二级缓存是指redis缓存, 是跨线程 跨tomcat实例的缓存(废话)

>##### 后面的getObject和listObjects查询方法, 获取实体对象DTO的优先级顺序是:
1. 先从一级缓存中获取, 有则直接返回
2. 一级缓存没有再从二级缓存Redis中获取, 有则返回;
3. 二级缓存也没有, 则生成SQL语句从数据库中获取. 

>##### getObject/listObjects的设计思想
- 尽可能的从一二级缓存中获取;
- 如果是从数据库查询出结果, 则都放入一级缓存中,且如果开启了二级缓存则也同时放入到二级缓存中.  

## getObject(Object id)
- @param id 传入的Id
- @return 返回DTO实体对象

根据Id值获得一个DTO实体对象

## getObject(Object id, boolean isCache)
- @param id 传入的Id
- @param isCache 是否从缓存中获取
    1. true 优先从一二级缓存中获取 
    2. false 始终执行SQL查询获取
- @return 返回DTO实体对象 

## listObjects(Object... ids)
- @param ids 传入的多个Id值
- @return 返回 List< DTO >

根据多个Id值获得对象

## listObjects(Object... ids, boolean isCache)
- @param ids 传入的多个Id值
- @param isCache 是否从一二级缓存中获取
- @return 返回 List< DTO >

## listObjects(Set<String> setIds) 
- @param setIds 传入的多个Id值
- @return 返回 List< DTO >

## listObjects(Set<String> ids, boolean isCache) {
- @param setIds 传入的多个Id值
- @param isCache 是否从一二级缓存中获取
- @return 返回 List< DTO >

## listObjects(QTO qto)
- @param qto 依据QTO生成WHERE条件的SQL, QTO字段间用 and连接
- @return 返回 List< DTO >

不会从二级缓存Redis中获取

## listObjects(Condition where)
- @param where 条件匹配器, 可参考后面的<条件匹配器Condition的用法>
- @return 返回 List< DTO >

> 不会从二级缓存Redis中获取
> 返回DTO实体的所有字段的值
> 自定义条件的查询, 使用条件匹配器Condition生成SQL条件, 功能最强大的查询, > 可以生成任何复杂的条件. 不会从缓存中获取.

## listObjects(String fields, Condition where)
```java
public List<DTO> listObjects(String fields, Condition where){
    return listObjects(fields, false, where);
}
```
当仅仅需要部分字段的值时, 请使用本方法.
从源码可以看出, 调用的是下一个方法

## listObjects(String fields, Boolean isAppendAllFields, Condition where)   
- @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段. 支持数据库函数对字段处理,如: substring(name,2)
- @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
- @param where 条件匹配器Condition对象
- @return 返回 List< DTO >

> 不会从Redis缓存中获取
> 自定义条件的查询
> 特别说明, 以上所有的 getObject/listObject本质都是调用的当前方法. 下面> 把类库的源码贴出来:

```java
public List<DTO> listObjects(String fields, Boolean isAppendAllFields, Condition where){
    //直接从数据库中查询出结果
    List<DTO> list = selectBatch(where, fields, isAppendAllFields);

    List<DTO> listBean =  new ArrayList<>();//定义返回的List对象
    if (list.isEmpty()){
        return listBean;
    }

    //对查询结果遍历了一次
    list.forEach((dto) -> {
        //根据Id从一级缓存中获取
        DTO cacheDTO = dataCache.getCacheBySelect(clazz, dto.dataId());
        if (cacheDTO != null) {//一级缓存找到对象, 则以一级缓存的为准,作为返回的DTO
            listBean.add(cacheDTO);
        }else {
            //fields为空, 默认返回所有字段, 所以可以更新缓存
            if(Misc.isNull(fields) || isAppendAllFields) {
                //添加一级缓存, 二级缓存(考虑版本号)
                dataCache.addCacheBySelected(dto, sysConfig.isOpenRedis());
            }
            listBean.add(dto);
        }
    });

    return listBean;
}
```
注意, 源码中调用了  selectBatch(Condition where, ...)
> 从源码我们可以看出来, 当从数据库查询出的结果当中, 如果某个DTO在一级缓存中已经存在, 则会舍去从数据库查询的DTO, 以一级缓存的DTO为准. 
> 为什么这么设计? 

# selectBatch和selectForListMap
与listObjects/getObject的区别是:
- 不会读写一二级缓存.
- 不会对查询结果遍历, 性能比listObjects getObject高.
- listObjects 本质也是调用selectBatch从数据库获得结果的

## selectBatch(Condition where)
- @param where 条件匹配器Condition对象
- @return 返回 List< DTO >

## selectBatch(String fields, Condition where)
- @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段
- @param where 条件匹配器Condition对象
- @return 返回 List< DTO >

## selectBatch(String fields, Boolean isAppendAllFields, Condition where)
- @param fields 指定查询返回的字段,多个用逗号串联, 为空时则返回所有DTO的字段. 支持数据库函数对字段处理,如: substring(name,2)
- @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
- @param where 条件匹配器Condition对象
- @return 返回 List< DTO >

## selectForListMap(Condition where)
返回 List< Map< String, Object >> 
直接从数据中获取数据, 这里不会读写一二级缓存

## selectForListMap(String fields, Condition where)
返回 List< Map< String, Object >> 
直接从数据中获取数据, 这里不会读写一二级缓存

## selectForListMap(String fields, Boolean isAppendAllFields, Condition where)
返回 List< Map< String, Object >> 
直接从数据中获取数据, 这里不会读写一二级缓存

## selectCount(Condition where)
- @param where
- @return 查询结果的行数

自定义条件的计数查询, 不会从缓存中获取
## selectMax(String fieldName, Condition where)
自定义条件, 获得指定字段名的最大值, 字段类型支持任何类型. 
源码:
```java
/**
     *
     * 注意,返回类型时泛型哦
     * @param fieldName 指定的字段名
     * @param where 条件匹配器
     * @param <T> 返回类型的泛型
     * @return
     */
    public <T> T  selectMax(String fieldName, Condition where){
        ...源码略...
    }
```

奇妙的用法:
```java
//这样写则将返回的结果转为 String型
String maxString = selectMax("字段名", where()...);

//这样写则将返回的结果转为 Long型
Long maxLong = selectMax("字段名", where()...);
...
```

# 条件匹配器Condition的用法
## between

```java
between(String fieldName, Object valueFrom, Object valueTo)
```
BETWEEN 值1 AND 值2
- 例 between("age", 18, 30) ---> age between 18 and 30

## eq 等于 =
```java
eq(String fieldName, Object value)
eq(String fieldName, Object... values)
eq(String fieldName, Set set)  //Set<String> Set<Long> Set<Integer>
eq(String fieldName, List list)//List<String> List<Long> List<Integer>
```

- eq("name", "罗平") ---> name = '罗平'
- eq("age", 18)     ---> age = 18
- eq("name", new Object[] {"A", "B"} ) ---> (name = 'A' OR name = 'B')
- eq("name", "C", "D", "E")  ---> (name = 'C'  OR name = 'D' OR name = 'E')

## like | likeLeft | likeRight
```java
like(String fieldName, String... values)
likeLeft(String fieldName, String... values) 
likeRight(String fieldName, String... values)
```
例:
- like("name","值")      ---> name LIKE '%值%'
- likeLeft("name","值")  ---> name LIKE '%值'
- likeRight("name","值") ---> name LIKE '值%'
- like("name","值A", "值B")  ---> (name LIKE '%值A%' OR name LIKE '%值B%')

## notLike | notLikeLeft | notLikeRight
```java
notLike(String fieldName, String... values)
notLikeLeft(String fieldName, String... values) 
notLikeRight(String fieldName, String... values)
```
例 略

## ne 不等 <>
```java
ne(String fieldName, Object... values)
```
 例:
- ne("age",11) ---> age <> 1
- ne("name","张三") ---> name <> '张三'
- ne("name","张三","李四") ---> (name <> '张三' AND name <> '李四')

## le 小于等于 <= 
```java
le(String fieldName, Object... values)
```
- 例 le("age",11) ---> age <= 1

## le 小于等于 <= 
```java
le(String fieldName, Object... values)
```
- 例 le("age",11) ---> age <= 1

## lt 小于 < 
```java
lt(String fieldName, Object... values)
```
- 例 lt("age",11) ---> age < 1

## ge 大于等于 >=
```java
ge(String fieldName, Object... values)
```
- 例 ge("age",11) ---> age >= 1

## gt 大于 >
```java
gt(String fieldName, Object... values)
```
- 例 gt("age",11) ---> age > 1

## isNull 字段 IS NULL
- 例: isNull("name") ---> name is null

## isNotNull 字段 IS NOT NULL
- 例: isNull("name") ---> name is not null        
                
# 条件匹配器Condition一个完整的例子
```java
package com.java.api;
...
@Service("userService_5")
public class C5_UserService extends SimpleService<UserQTO, UserDTO> {
	...
    public void test(){
        //**查询 返回对象 */
        List<UserDTO> listUsers = listObjects(
            where()
                .between("age",10,20)
                .eq("name","C","D","E")//生成 AND(name = 'C'  OR name = 'D' OR name = 'E')
                .like("name","A", "B") //生成 AND(name LIKE '%A%' OR name LIKE '%B%')
        
                //不等
                .ne("name","张三","李四")
        
                //自定义括号
                .and("(")  //生成: and(
                .eq("amount",10.10)
                .or()//在此以后的所有字段都用 or
                .eq("loginCount", 10)
                .le("loginCount",11)
                .add(")")// add 条件总能增加任意字符
                
                .and()//因为前面写了 .or(),我们希望括号外面以后的的字段用and
        
                .add("substring(name,2)='平' ")//自定义条件,要用到数据库的函数时可以这样写
                .le("loginCount",1)//小于等于
                .lt("loginCount",2)//小于
                .ge("loginCount",4)//大于等于
                .gt("loginCount",3)//大于
        
                .isNull("name")
                .isNotNull("name")
        );  
    }
}

```
生成的WHERE条件如下:

```SQL
SELECT ... 
FROM SecUser a
WHERE (age BETWEEN 10 AND 20)
  AND (name = 'C' OR name = 'D' OR name = 'E')
  AND (name LIKE '%A%' OR name LIKE '%B%')
  AND (name <> '张三' AND name <> '李四')
  AND ((amount = 10.1) OR (loginCount = 10) OR (loginCount <= 11))
  AND substring(name,2)='平' 
  AND (loginCount <= 1)
  AND (loginCount < 2)
  AND (loginCount >= 4)
  AND (loginCount > 3)
  AND name is null
  AND name is not null
```

# com.topfox.spring.AdvancedService
AdvancedService是继承 SimpleService的, 提供了用得少, 自由度更灵活强大的一些功能.

## updateBatch(DTO xxxDTO, Condition where)
- @param xxxDTO 要更新的数据, 不为空的字段才会更新. Id字段不能传值
- @param where  条件匹配器
- @return 更新的行数

用条件匹配器生成SQL条件更新, 影响到多条记录时用此方法 
解决多条记录要更新为同样的值(数据载体xxxDTO), 只生成一条SQL时用此方法

>技巧:
>如何将某些字段更新为null值呢? 代码如下, 更新之前这样写:
xxxDTO.mapModify().put("字段名",null);

完整一点的源码:

```java
    UserDTO userDTO = new UserDTO();
    userDTO.setAge(30);
    userDTO.mapModify().put("mobile",null);

    //把性别=男的记录 age更新为30, mobile更新为null
    updateBatch(userDTO, where().eq("sex","男"));
```

## deleteBatch(Condition where)
用条件匹配器生成SQL条件去删除, 影响到多条记录时用此方法

## list(QTO xxxQTO)
- @param xxxQTO 查询的条件对象
- @return 返回结果 Response< List< DTO>>

优先查找 xxxMapple.xml里面, < select id = "list">的SQL, 如果没有, 则类库会根据DTO自动生成所有字段的SQL查询. 

## listCount(QTO xxxQTO)
与list(QTO xxxQTO)配套的分页用总行数查询的SQL. 

## query1-8(QTO qto)
- @param xxxQTO 查询的条件对象
- @return 返回结果 Response< List< DTO>>

在xxxMapple.xml自定义的其他查询SQL

# com.topfox.common.Response< T > 返回结果对象
服务调用服务和前端调用后端的放回结果, 统一用此对象.

## 主要属性
- boolean success 是否成功
- String code 异常编码
- String msg 详细信息
- String executeId 前端(请求方)每次请求传入的 唯一 执行Id, 如果没有, 后端会自动生成一个唯一的
- Integer totalCount 
    1. 查询结果的所有页的行数合计
    2. update insert delete SQL执行结果影响的记录数
- Integer pageCount 查询结果(当前页)的行数合计
- T data 泛型.  数据, List<xxxDTO> 或者是一个DTO
- String exception  失败时, 出现异常类的名字
- String method 失败时, 出现异常的方法及行数
- JSONObject attributes 自定义的数据返回, 这部分数据不方便定义到"T data"中时使用


## 构造函数(无业务数据)
出错抛出异常时使用
- Response(Throwable e)
- Response(Throwable e, ResponseCode responseCode, String msg)
- Response(Throwable e, ResponseCode responseCode)
- Response(ResponseCode responseCode, String msg)
- Response(ResponseCode responseCode)
- Response(String code, String msg)

## 构造函数(需要返回业务数据)
查询或者更新插入删除返回数据时使用.
- Response(T data)
- Response(T data, Integer totalCount)
    data为List<xxxDTO>, list.size()会设置为当前页行数pageCount, 总行数通过第2个参数传入进来. 本构造函数分页查询时常用.
    
    
## 数据格式示例
- 失败出错返回的数据

```json

{
    "success": false,
    "code": "30020",
    "msg": "密码不正确,请重试",
    "executeId": "190409103131U951KMD495",
    "exception": "com.topfox.common.CommonException",
    "method": "com.sec.service.UserService.login:190"
}
```

- 登陆成功返回的数据(attributes为权限信息):

```json
{
	"success": true,
	"code": "200",
	"msg": "请求成功",
	"executeId": "190409102553Y303TYC041",
	"totalCount": 1,
	"data": {
		"userId": "00022",
		"userName": "张三",
		"userCode": "00384",
		"userMobile": "*",
		"isAdmin": true
	},
	"attributes": {
		"secString": {
			"programList": {
				"btnSave": "0",
				"btnNew": "0",
				"btnDelete": "1",
				"btnExport": "1",
				"btnImport": "1"
			},
			"userProgramList": {
				"btnSave": "1",
				"btnNew": "0",
				"btnDelete": "0"
			}
		},
		"sessionId": "190409022553381R786S343"
	}
}
```

