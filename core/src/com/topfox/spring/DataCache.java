package com.topfox.spring;

import com.topfox.common.DataDTO;
import com.topfox.common.IBean;
import com.topfox.common.SysConfigRead;
import com.topfox.conf.CustomRedisTemplate;
import com.topfox.data.DbState;
import com.topfox.data.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;

import java.util.HashMap;
import java.util.Map;

/**
 * 一级缓存: 当前线程缓存 localCache
 * 二级缓存: redis缓存
 */

public final class DataCache {
	public HashMap<String,Map<String,DataDTO>> localCacheBySave   = new HashMap(); //增删改放入的一级缓存
	public HashMap<String,Map<String,DataDTO>> localCacheBySelect = new HashMap(); //查询放入的一级缓存
	private HashOperations<String, String, IBean> redisHOP;
	private CustomRedisTemplate sysRedisTemplateDTO;
	private SysConfigRead sysConfig;

	private static final Logger logger = LoggerFactory.getLogger(DataCache.class);
	//构造函数
	public DataCache(CustomRedisTemplate sysRedisTemplateDTO, SysConfigRead sysConfig){
		if (sysRedisTemplateDTO==null){
			this.redisHOP=null;
		}else {
			this.sysRedisTemplateDTO=sysRedisTemplateDTO;
			this.redisHOP = sysRedisTemplateDTO.opsForHash();
		}
		this.sysConfig = sysConfig;
	}

	/**
	 * 该方法先去一级缓存(从当前线程)，再取二级缓存(Redis)
	 * @bean 只有Id字段有值
	 */
	public <T extends DataDTO> T get2CacheById(Class<T> clazz, Object id, boolean openRedis, String method) {
		String idString = (id instanceof String)?id.toString():String.valueOf(id);
		T beanLocal = getCacheBySelect(clazz, idString);      //获取一级缓存---本线程缓存
        T beanRedis = openRedis?getRedis(clazz,idString):null;//获取二级缓存---Reids缓存
		if (beanLocal ==null && beanRedis == null) return null;

		//命中缓存的打印信息
		StringBuilder sbPrint = new StringBuilder();
		sbPrint.append(sysConfig.getLogPrefix())
				.append(beanLocal!=null?"命中一级缓存(当前线程)":"命中二级缓存(Redis), 并放入一级缓存 ")
				.append("hashCode=")
				.append(beanLocal!=null?beanLocal.hashCode():beanRedis.hashCode()).append(" ")
				.append(clazz.getName()).append(" ").append(method)
				.append("(").append(idString).append(")");

		if (beanLocal!=null && beanRedis!=null && beanLocal.dataVersion()!=null
				&& beanLocal.dataVersion() >= beanRedis.dataVersion())
        {
			//rowRedis==null 或者  本线程缓存对象的修改时间>=Redis缓存数据的修改时间
			logger.debug(sbPrint.append(" version=").append(beanLocal.dataVersion()).toString());
			return beanLocal;//则返回一级对象
        }

		if (beanLocal!=null ) {
			if (beanLocal.dataVersion()==null)
				logger.debug(sbPrint.toString());
			else
				logger.debug(sbPrint.append(" version=").append(beanLocal.dataVersion()).toString());
			return beanLocal;//则返回一级对象
		}else if (beanRedis!=null ) {
			if (beanRedis.dataVersion()==null)
				logger.debug(sbPrint.toString());
			else
				logger.debug(sbPrint.append(" version=").append(beanRedis.dataVersion()).toString());

			addCacheBySelected(beanRedis,true);//命中二级redis缓存, 则放入一级缓存. 目的为了获取时取得同一个实例对象
			return beanRedis;
		}

		return null;
	}

	/** 缓存set**************************************************************************************************
	 * 立即放入redis缓存,用此方法
	 * */
	public void saveRedis(DataDTO dto) {
		if (dto==null || redisHOP==null) return;
		saveRedis(dto, TableInfo.get(dto.getClass()));
	}
	public void saveRedis(DataDTO dto, TableInfo tableInfo) {
		if (dto==null || redisHOP==null) return;

		String dbState = dto.dataState();

		if (DbState.UPDATE.equals(dbState) || DbState.INSERT.equals(dbState)) {
			redisHOP.put(tableInfo.getRedisKey(),dto.dataId(),dto);
			logger.debug("{}{}后写入Redis成功 {} hashCode={} id={} version={}",
					sysConfig.getLogPrefix(),
					DbState.INSERT.equals(dbState)?"插入":"更新",
					tableInfo.getRedisKey(),
					dto.hashCode(),
					dto.dataId(),
					dto.dataVersion());
		} else if (DbState.DELETE.equals(dbState)) {
			deleteRedis(tableInfo, dto.dataId(),tableInfo.openRedis());//放入二级缓存redis
		}else { //if (getCacheByModified(dto.getClass(), dto.dataId()) == null)
			/**
			 * 说明是查询后, 执行了放入缓存
			 * 在当前线程中, 执行某个查询后要写入缓存时,如果该DTO如何已经修改过, 则不能放入二级缓存
			 * 因为能查到当前线程有修改但未commit的数据, 写入二级缓存后,就是脏数据
			 */

			//是否要 检查版本号,  放入的DTO版本号 大于等于redis的, 则放入
			redisHOP.put(tableInfo.getRedisKey(),dto.dataId(),dto);
			logger.debug("{}查询后写入Redis成功 {} hashCode={} id={}",
					sysConfig.getLogPrefix(),tableInfo.getRedisKey(), dto.hashCode(),dto.dataId());
		}
	}

	public Long  deleteRedis(TableInfo tableInfo,Object idValue, boolean openRedis) {
		if (openRedis == false || tableInfo == null || redisHOP == null) return 0L;
		Long result=redisHOP.delete(tableInfo.getRedisKey(),idValue);
		logger.debug("{} 删除Redis成功 {} id={}",sysConfig.getLogPrefix(), tableInfo.getRedisKey(), idValue);
		return result;
	}

	//缓存  get ****************************************************************************************************
	public <T extends DataDTO> T getRedis(Class<T> clazz,Object idValue) {
		if (clazz==null || idValue==null) return null;
        return (T)sysRedisTemplateDTO.opsForHash().get(TableInfo.get(clazz).getRedisKey(),idValue);
	}
	//缓存  get 完毕************************************************************************************************



	//*************************************************************************************************************
	//get-----------------------------------------------------------------------------------------------------------
	/**  从一级缓存获取查询放入的缓存对象，Request级别 */
	public <T> T getCacheBySelect(Class<T> clazz,Object id){
		return getCacheData(clazz, localCacheBySelect, id);
	}
	/** 获取增删改的缓存对象，Request级别. 更新redis用 */
	public <T> T getCacheBySave(Class<T> clazz,Object id){
		return getCacheData(clazz, localCacheBySave, id);
	}

	/** 内部方法 从一级缓存获取，Request级别 */
    private <T> T getCacheData(Class<T> clazz, HashMap<String,Map<String,DataDTO>> cacheData, Object id){
		Map<String,DataDTO> cacheMap = cacheData.get(clazz.getName());
		if (cacheMap == null) return null;
		Object object = cacheMap.get(id);
		return object == null?null:(T)object;
	}

	/**
	 * 一级缓存
	 * @param cacheData
	 * @param bean
	 * @param openRedis
	 * @return
	 */
	private Map<String,DataDTO> addCache(HashMap<String,Map<String,DataDTO>> cacheData, DataDTO bean, Boolean openRedis) {
		Map<String,DataDTO> cacheMap = cacheData.get(bean.getClass().getName());
		if (cacheMap==null){
			cacheMap=new HashMap();
			cacheData.put(bean.getClass().getName(), cacheMap);
		}
		cacheMap.put(bean.dataId(),bean);//放入一级缓存
		return cacheMap;
	}

	//add ###########################################################################################################
	/** 查询时 add 一个一级 缓存 */
	public void addCacheBySelected(DataDTO bean, Boolean openRedis){
	    if (bean==null || bean.dataId()==null) return;
		addCache(localCacheBySelect, bean, openRedis);
	}
	//add ###########################################################################################################
	/** insert/update时 add 一级缓存, 更新Redis使用*/
	public void addCacheBySave(DataDTO bean, Boolean openRedis){
        if (bean==null || bean.dataId()==null) return;
        addCacheBySelected(bean, openRedis);//解决 订单头 save（bean）后，明细selectbyId 拿不到对象的错误。
		addCache(localCacheBySave, bean, openRedis); //放入一级缓存
	}


	public boolean commitRedis() {
		// 同一个DTO在 localCacheBySelect 和 localCacheBySave都存在时, 以localCacheBySave为准, 因此这里需要合并
		//用 localCacheBySave 的数据 替换到 localCacheBySelect中
		for (String clazzName : localCacheBySave.keySet()) {
			TableInfo tableInfo = TableInfo.get(clazzName);
			Map<String, DataDTO> mapSave = localCacheBySave.get(clazzName);
			mapSave.forEach((id, dto)->{
				if(dto==null) return;
				addCacheBySelected(dto, tableInfo.openRedis());
			});
		}
		commitRedis(localCacheBySelect);
		return true;
	}


	/**
	 * 提交redis的修改. 在数据库的修改 提交后, 即拦截器里面调用 本方法
	 * 只会提交有增删改的DTO对象
	 */
	private boolean commitRedis(HashMap<String,Map<String,DataDTO>> localCache) {
		for (String clazzName : localCache.keySet()) {
			TableInfo tableInfo = TableInfo.get(clazzName);
			if (tableInfo.openRedis() == false) continue;//没有开启redis

			Map<String, DataDTO> mapCacheData = localCache.get(clazzName);
			mapCacheData.forEach((id, dto)->{
				if(dto==null) return;
				if (redisHOP != null) {
					saveRedis(dto, tableInfo);
				}
			});
		}
		return true;
	}


//	public boolean redisRollack() {
//		if (open == false) return false;
//		for (Class<?> clazz : localCacheBySave.keySet()) {
//			Map<String, Object> cacheHashList = localCacheBySave.get(clazz);
//			Dataset dataset = getDatasetInfo(clazz);
//			for (int i = 0; i < cacheHashList.size(); i++) {
//				Object object = cacheHashList.get(i);
//				if(object==null)continue;
//				if (object instanceof BaseBean) {
//					String key = getCacheKeyID(clazz, ((BaseBean)object).getId());
//					RedisClient.del(key);
//				} else {
//					JSONObject row = (JSONObject) object;
//					String key = getCacheKeyID(clazz, row.getString(dataset.getIdField()));
//					RedisClient.del(key);
//				}
//			}
//		}
//		return true;
//	}
}
