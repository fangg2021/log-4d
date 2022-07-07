package test.log.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import test.log.constant.Log4dConstant;

/**
 * 缓存RpcContext上下文信息
 * @author fangg
 * 2022年7月3日 上午9:40:03
 */
public class RpcCacheContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static ThreadLocal<Map<String, Object>> traceContextHolder = new ThreadLocal<Map<String, Object>>();

	/**
	 * 缓存rpc上下文信息rpcInfo、当前接口信息serviceInfo、是否注解有@LogAnnotation信息isLogAnno
	 */
	public static ThreadLocal<Map<String, Object>> get() {
		return traceContextHolder;
	}

	/**
	 * 设置traceContext
	 */
	public static void setTraceContext(Map<String, Object> context) {
		traceContextHolder.set(context);
	}

	/**
	 * 获取traceContext
	 */
	public static Map<String, Object> getTraceContext() {
		return traceContextHolder.get();
	}
	
	/**
	 * 缓存rpc上下文信息
	 */
	public static void setRpcInfo(Object context) {
		if (traceContextHolder.get() == null) {
			setTraceContext(new HashMap<String, Object>());
		}
		traceContextHolder.get().put("rpcInfo", context);
	}
	
	/**
	 * 取缓存rpc上下文信息
	 */
	public static String getRpcInfo() {
		if (traceContextHolder.get() != null 
				&& traceContextHolder.get().containsKey("rpcInfo")) {
			return JSON.toJSONString(traceContextHolder.get().get("rpcInfo"));
		}
		return null;
	}
	
	/**
	 * 缓存当前接口对应的上下文信息
	 */
	public static void setServiceInfo(Object context) {
		if (traceContextHolder.get() == null) {
			setTraceContext(new HashMap<String, Object>());
		}
		traceContextHolder.get().put("serviceInfo", context);
	}
	
	/**
	 * 取当前接口对应的上下文信息
	 */
	public static String getServiceInfo() {
		if (traceContextHolder.get() != null 
				&& traceContextHolder.get().containsKey("serviceInfo")) {
			return JSON.toJSONString(traceContextHolder.get().get("serviceInfo"));
		}
		return null;
	}
	
	/**
	 * 注解有@LogAnnotation
	 */
	public static void setLogAnnotation() {
		if (traceContextHolder.get() == null) {
			setTraceContext(new HashMap<String, Object>());
		}
		traceContextHolder.get().put("isLogAnno", true);
	}
	
	/**
	 * 当前接口是否注解有@LogAnnotation(默认false)
	 */
	public static boolean isLogAnnotation() {
		if (traceContextHolder.get() != null 
				&& traceContextHolder.get().containsKey("isLogAnno")) {
			return true;
		}
		return false;
	}
	
	/**
	 * 获取完整服务接口的上下文信息(如果之前没有上下文信息，则取当前接口的)
	 */
	public static String getAllServiceContext() {
		String context = getRpcInfo();
		if (context != null) {
			if (getServiceInfo() != null) {
				Map<String, Object> map = JSONObject.parseObject(context);
				OperateLogTO operateLog = null;
	            for (Map.Entry<String, Object> entry : map.entrySet()) {
	                String key = entry.getKey();
	                if (key.startsWith("LOG_")) {
	                    operateLog = JSONObject.parseObject(String.valueOf(entry.getValue()), OperateLogTO.class);
	    				OperateLogTO operateLogThis = getOperateLogEntity(getServiceInfo());

	    	            if (operateLogThis != null) {
							operateLog.setParamsInfo(operateLog.getParamsInfo() +"&"+ operateLogThis.getParamsInfo());
							operateLog.setRunInfo(operateLog.getRunInfo() +"&"+ operateLogThis.getRunInfo());
							if (Log4dConstant.DEFAULT.equals(operateLog.getUserCode()) 
									&& Log4dConstant.DEFAULT.equals(operateLogThis.getUserCode()) == false) {
								operateLog.setUserCode(operateLogThis.getUserCode());
							}
							map.put(key, operateLog);
							return JSON.toJSONString(map);
						}
	    	            return context;
	                }
	            }
	            return getServiceInfo();
			}
			return context;
		}
		return getServiceInfo();
	}
	
	/**
	 * 从缓存信息中直接取OperateLogTO对象
	 */
	public static OperateLogTO getOperateLogEntity(String context) {
		if (StringUtils.isNotEmpty(context)) {
			Map<String, Object> mapThis = JSONObject.parseObject(context);
	        for (Map.Entry<String, Object> entryThis : mapThis.entrySet()) {
	            String keyThis = entryThis.getKey();
	            if (keyThis.startsWith("LOG_")) {
	            	return JSONObject.parseObject(String.valueOf(entryThis.getValue()), OperateLogTO.class);
	            }
	        }
		}
		return null;
	}

	/**
	 * 清空trace上下文
	 */
	public static void clear() {
		traceContextHolder.remove();
	}

}
