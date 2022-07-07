package test.log.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import test.log.annotation.LogAnnotation;
import test.log.constant.Log4dConstant;
import test.log.entity.OperateLogTO;
import test.log.entity.RpcCacheContext;

/**
 * dubbo拦截器处理 
 * @author fangg
 * 2022年7月4日 下午2:05:59
 */
public class DubboFilterUtil {
    protected static final Logger logger = LoggerFactory.getLogger(DubboFilterUtil.class);

	/**
	 * 判断LogAnnotation（只对comsumer有效）
	 * @param filterClassUrl 需要匹配@LogAnnotation所有方法的类路径
	 */
	public static boolean checkLogAnnotation(String filterClassUrl, String serviceMethodName) {
		Map<String, Object> annotationParamsMap = new HashMap<String, Object>();
		return checkLogAnnotation(filterClassUrl, serviceMethodName, annotationParamsMap);
	}
	
	/**
	 * 判断LogAnnotation（只对comsumer有效）
	 * @param filterClassUrl 需要匹配@LogAnnotation所有方法的类路径
	 * @param serviceMethodName 接口方法名
	 * @param annotationParamsInfoMap 返回@LogAnnotation的参数
	 */
	public static boolean checkLogAnnotation(String filterClassUrl, String serviceMethodName, Map<String, Object> annotationParamsInfoMap) {
		StackTraceElement stack[] = Thread.currentThread().getStackTrace();
		Class cls = null;
		String methodName = null;
		
		// 判断filterClassUrl开头的上级目录
		try {
			for (StackTraceElement parent : stack) {
				if (classUrlMatch(parent.getClassName(), filterClassUrl)) {
					cls = Class.forName(parent.getClassName());
					methodName = parent.getMethodName();
					break;
				}
			}
			
			if (cls != null && checkLogAnnotation(cls, serviceMethodName, methodName, annotationParamsInfoMap)) {
				annotationParamsInfoMap.put("classPath", cls.getName()+"."+methodName);
				return true;
			}
		} catch (ClassNotFoundException e) {
			logger.warn("判断@LogAnnotation异常：\n" + e.getMessage());
			throw new RuntimeException("判断@LogAnnotation异常：\n" + e.getMessage());
		} catch (Exception e) {
			logger.warn("判断@LogAnnotation异常：\n" + e.getMessage());
		}
		return false;
	}
	

	/**
	 * 匹配指定的类路径
	 */
	public static boolean classUrlMatch(String fileName, String filterUrl) {
		if (StringUtils.isNotEmpty(filterUrl)) {
			if (filterUrl.indexOf(",") != -1) {
				String [] temp = filterUrl.split(",");
				for (String str : temp) {
					if (fileName.startsWith(str)) {
						return true;
					}
				}
			}
			return fileName.startsWith(filterUrl);
		}
		return false;
	}

	/**
	 * 通过类反射机制判断logAnnotation注解
	 * @param cls controller类
	 * @param serviceName  service方法名称
	 * @param methodName 调用service接口的 controller方法名称(必须为public)
	 */
	private static boolean checkLogAnnotation(Class cls, String serviceName, String methodName, Map<String, Object> paramsMap) {
		try {
			Method [] methods = cls.getMethods();
			LogAnnotation logAnnotation = null;
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					logAnnotation = method.getAnnotation(LogAnnotation.class);
					if (logAnnotation != null) {
						// 方法参数信息
						List<String> parameterNameList = new ArrayList<String>();
						if (method.getParameters().length > 0) {
							for (Parameter parameter : method.getParameters()) {
								parameterNameList.add(parameter.getDeclaringExecutable().getName());
							}
						}
						paramsMap.put("methodParametersType", method.getParameterTypes());
						paramsMap.put("methodParameters", parameterNameList);
						paramsMap.put("userCodeFlag", logAnnotation.userCodeFlag());
						
						String [] serviceTemp = logAnnotation.serviceMethod();
						// 如果serviceMethod参数为空，表示匹配所有方法
						if (serviceTemp.length > 0) {
							for (String name : serviceTemp) {
								if (serviceName.equals(name)) {
									paramsMap.put("function", logAnnotation.function());
									paramsMap.put("write", logAnnotation.write());
									return true;
								}
							}
						} else {
							paramsMap.put("function", logAnnotation.function());
							paramsMap.put("write", logAnnotation.write());
							return true;
						}
					}
					return false;
				}
			}
		} catch (Exception e) {
			logger.warn("通过类反射机制判断logAnnotation注解异常：\n" + e.getMessage());
		}
		return false;
	}
	


	/**
	 * rpcContext上下文参数处理
	 * <pre></pre>
	 */
	public static void checkRpcContext(Object [] arguments, Map<String, Object> paramsMap, Map<String, Object> rpcAttachmentsMap, 
			String ipAddr, String dubboAppName) {
		OperateLogTO operateLogVO = null;
//		logger.info("consumer invocation信息：{}", JSONObject.toJSONString(invocation.getAttachments()));
//		logger.info("consumer rpcContext信息：{}", JSONObject.toJSONString(rpcContext.getObjectAttachments()));
		
		//invocation.getAttachments().putAll(rpcContext.getAttachments());
		// 从缓存中取上下文信息
		boolean emptyFlag = true;
		Map<String, Object> paramMap = rpcAttachmentsMap;
		if (paramMap.isEmpty() == false) {
			for (Entry<String, Object> entry : paramMap.entrySet()) {
				if (entry.getKey().startsWith("LOG_")) {
					emptyFlag = false;
					break;
				}
			}
		}
		
		if (emptyFlag) {
			String contextInfo = RpcCacheContext.getRpcInfo();
			if (StringUtils.isNotEmpty(contextInfo)) {
				JSONObject jsonObject = JSONObject.parseObject(contextInfo);
				paramMap = new HashMap<String, Object>();
				emptyFlag = false;
				
				for (Entry<String, Object> entry : jsonObject.entrySet()) {
					paramMap.put(entry.getKey(), String.valueOf(entry.getValue()));
				}
				rpcAttachmentsMap.putAll(paramMap);
			}
		}
		
//		if (redisDbTemplate == null || redisDbTemplate.isValidConnect(0) == false) {
//			redisDbTemplate = SpringContextUtil.getBean(RedisDbTemplate.class);
//		}

		// 当前上下文信息处理
		// 如果没有上下文信息，则需要取当前接口的信息为主信息
		if (emptyFlag) {
			// 取操作人，如果接口参数中不存在操作人(即userCode参数)，则取SYSTEM_CODE
			// [{"userCode":"test b"}]
			String userCode = null;
			if (arguments.length > 0) {
				userCode = checkUserCode(arguments, paramsMap);
			}
			if (userCode == null) {
				userCode = Log4dConstant.DEFAULT;
			}
			
			// 要传递的上下文信息
			operateLogVO = new OperateLogTO();
			operateLogVO.setUserCode(userCode);
			operateLogVO.setParamsInfo(JSONObject.toJSONString(arguments));
			operateLogVO.setFunctionInfo(String.valueOf(paramsMap.get("function")));
			// 本地IP
			//operateLogVO.setIpAddr(NetUtils.getLocalAddress().getHostAddress());
			// 外网IP
			operateLogVO.setIpAddr(ipAddr);
			operateLogVO.setRunInfo(DateUtil.getTodayToSecond() + "|" +operateLogVO.getIpAddr()+"|"
					+ String.valueOf(paramsMap.get("classPath"))+"()|"+String.valueOf(paramsMap.get("function"))
					+ "|" +dubboAppName);
			//operateLogVO.setServiceMethod(invocation.getMethodName());
			// 这里无法判断入口网络类型 
			//operateLogVO.setInType(0);
			//logger.info("新生成的上下文信息：{}", JSONObject.toJSONString(operateLogVO));
			
			// 上下文信息放到rpcContext中
			rpcAttachmentsMap.put("LOG_"+operateLogVO.getUserCode(), JSONObject.toJSONString(operateLogVO));
			
			// 缓存上下文信息
			RpcCacheContext.setTraceContext(rpcAttachmentsMap);
		} 
		// 加上当前服务接口的信息
		else {
			for (Entry<String, Object> entry : rpcAttachmentsMap.entrySet()) {
				if (entry.getKey().startsWith("LOG_")) {
					/*
					 * {"LOG_A6d424268":"{\"function\":\"测试操作日记\",\"inType\":0,\"ipAddr\":\"169.254.172.203\",
					 * \"paramsInfo\":\"[{\\\"userCode\\\":\\\"test b\\\"}]\",\"runInfo\":\"com.test.controller.LoginController.logTest\",
					 * \"serviceMethod\":\"updateTest\",\"userCode\":\"A6d424268\"}","input":"684","interface":"com.test.service.UserConfigService"}
					 */
					operateLogVO = JSONObject.parseObject(String.valueOf(entry.getValue()), OperateLogTO.class);
					// 更新执行路径
					operateLogVO.setRunInfo(operateLogVO.getRunInfo() +"&" + DateUtil.getTodayToSecond() 
						+ "|" +ipAddr+"|"
    					+ String.valueOf(paramsMap.get("classPath"))+"()|"+String.valueOf(paramsMap.get("function"))
    					+ "|" +dubboAppName);
					// 更新接口参数信息
					operateLogVO.setParamsInfo(operateLogVO.getParamsInfo() + "&" + JSONObject.toJSONString(arguments));
					
					rpcAttachmentsMap.put(entry.getKey(), JSONObject.toJSONString(operateLogVO));
				}
			}
			//rpcContext.setAttachments(rpcMap);
		}
	}

	/**
	 * 判断参数中是否存在userCode
	 */
	public static String checkUserCode(Object [] arguments, Map<String, Object> paramsMap) {
		String userCodeFlag = paramsMap.get("userCodeFlag")==null?"userCode":String.valueOf(paramsMap.get("userCodeFlag"));
		String userCode = null;
		String paramStr = JSONObject.toJSONString(arguments);
		if (paramStr.indexOf("\""+userCodeFlag+"\":") != -1 || paramStr.indexOf("\""+userCodeFlag+"\\\":") != -1) {
			JSONArray parameterArr = JSONArray.parseArray(paramStr);
			JSONObject paramJson = null;
			for (Object paramObj : parameterArr) {
				try {
					paramJson = JSONObject.parseObject(String.valueOf(paramObj));
					if (paramJson != null && paramJson.containsKey(userCodeFlag)) {
						userCode = paramJson.getString(userCodeFlag);
						break;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		
		if (userCode == null) {
			JSONArray paramNameArr = JSONArray.parseArray(String.valueOf(paramsMap.get("methodParameters")));
			if (paramNameArr != null && paramNameArr.contains(userCodeFlag)) {
				JSONArray paramNameTypeArr = JSONArray.parseArray(String.valueOf(paramsMap.get("methodParametersType")));
				for (int i = 0,size=paramNameArr.size(); i < size; i++) {
					if (userCodeFlag.equals(paramNameArr.getString(i)) 
							&& "java.lang.String".equals(paramNameTypeArr.getString(i))) {
						JSONArray parameterArr = JSONArray.parseArray(paramStr);
						userCode = parameterArr.getString(i);
					}
				}
			}
		}

		if (userCode == null) {
			userCode = Log4dConstant.DEFAULT;
		}
		return userCode;
	}

	/**
	 * 在AOP中判断userCode
	 */
    public static String checkUserCodeByAOP(Object[] args, LogAnnotation logAnnotation, String filterClassUrl) {
    	String userCodeFlag = logAnnotation.userCodeFlag();
		String userCode = null;
		String paramStr = JSONObject.toJSONString(args);
		if (paramStr.indexOf("\""+userCodeFlag+"\":") != -1 || paramStr.indexOf("\""+userCodeFlag+"\\\":") != -1) {
			JSONArray parameterArr = JSONArray.parseArray(paramStr);
			JSONObject paramJson = null;
			for (Object paramObj : parameterArr) {
				try {
					paramJson = JSONObject.parseObject(String.valueOf(paramObj));
					if (paramJson != null && paramJson.containsKey(userCodeFlag)) {
						return paramJson.getString(userCodeFlag);
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		
		// 判断String类型参数 
		if (userCode == null) {
			StackTraceElement stack[] = Thread.currentThread().getStackTrace();
			Class cls = null;
			String methodName = null;
			
			// 判断filterClassUrl开头的上级目录
			try {
				for (StackTraceElement parent : stack) {
					if (classUrlMatch(parent.getClassName(), filterClassUrl)) {
						cls = Class.forName(parent.getClassName());
						methodName = parent.getMethodName();
						break;
					}
				}
				
				if (cls != null) {
					Method [] methods = cls.getMethods();
					for (Method method : methods) {
						if (method.getName().equals(methodName)) {
							Parameter parameter = null;
							Parameter[] parameters = method.getParameters();
							for (int i = 0; i < parameters.length; i++) {
								parameter = parameters[i];
								if (userCodeFlag.equals(parameter.getDeclaringExecutable().getName())) {
									return String.valueOf(args[i]);
								}
							}
						}
					}
				}
			} catch (ClassNotFoundException e) {
				logger.warn("判断@LogAnnotation异常：\n" + e.getMessage());
			} catch (Exception e) {
				logger.warn("判断@LogAnnotation异常：\n" + e.getMessage());
			}
		}
		
		return userCode==null?Log4dConstant.DEFAULT:userCode;
	}
	
}
