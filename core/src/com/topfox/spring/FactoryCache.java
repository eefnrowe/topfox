//package com.topfox.spring;
//
//import com.topfox.common.CommonException;
//import com.topfox.common.ResponseCode;
//import com.topfox.conf.CustomRedisTemplate;
//import java.lang.ref.Reference;
//import java.lang.ref.WeakReference;
//
///**
// * 类库封装用的对象缓存, 开发者禁止修改缓存对象的信息,否则可能出乱子
// */
//public class FactoryCache {
//    /**
//     * 当前线程的缓存
//     */
//    private static ThreadLocal<DataCache> threadLocalDataCache = new ThreadLocal();
////    public static DataCache getDataCache(CustomRedisTemplate redisTemplate, SysConfig sysConfig) {
////        Reference<DataCache> ref = threadLocalDataCache.get();
////        DataCache dataCache=(ref==null?null:ref.get());
////        if (ref == null || dataCache==null) {
////            dataCache = new DataCache(redisTemplate, sysConfig);
////            threadLocalDataCache.set(new WeakReference(dataCache));
////        }
////
////        return dataCache;
////    }
//
//    public static DataCache getDataCache() {
////        Reference<DataCache> ref = threadLocalDataCache.get();
//        DataCache dataCache=threadLocalDataCache.get();
//        if (dataCache==null) {
//            // 2019-03-27 throw CommonException.newInstance(ResponseCode.ERROR).text("获取当前线程DataCache失败");
////            dataCache = new DataCache(redisTemplate, sysConfig);
////            threadLocalDataCache.set(new WeakReference(dataCache));
//        }
//
//        return dataCache;
//    }
//
//    public static void setDataCache(DataCache dataCache) {
//        threadLocalDataCache.set(dataCache);
//    }
//
//    /**
//     * 用完之后一定要释放,否则可能会造成 内存泄漏
//     */
//    public static void disposeDataCache() {
//        threadLocalDataCache.set(null);
//        threadLocalDataCache.remove();
//    }
//}
