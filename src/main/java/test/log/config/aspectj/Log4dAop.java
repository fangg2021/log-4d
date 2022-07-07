package test.log.config.aspectj;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.dubbo.common.utils.NetUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

import test.log.annotation.LogAnnotation;
import test.log.entity.OperateLogTO;
import test.log.entity.RpcCacheContext;
import test.log.util.DateUtil;
import test.log.util.DubboFilterUtil;
import test.log.util.HttpUtil;

/**
 * 操作日记AOP
 * @author fangg
 * 2022年7月6日 下午1:11:14
 */
@Component
@PropertySource("classpath:log4d.properties")
@Aspect
@Order(1)
public class Log4dAop {
	private static final Logger logger = LoggerFactory.getLogger(Log4dAop.class);
	
	@Value("${dubbo.application.name}")
	String dubboAppName;
	@Value("${log4d.filter.url}")
	String filterClassUrl;

	// 匹配所有有@LogAnnotation的方法
//	@Pointcut("execution(* com.test.log.service.impl.*.*(..))")
//    public void point() { }
	@Pointcut("@annotation(test.log.annotation.LogAnnotation)")
	public void point() { }
	

    /**
     * 前置通知
     * 连接点之前执行
     */
    @Before("point() && @annotation(logAnnotation)")
    public void before(JoinPoint joinPoint, LogAnnotation logAnnotation) {
    	// 是否已经过注解判断(如果当前接口里面调用当前类下面的别的接口B，而B接口也有@LogAnnotation时不再判断)
        if (RpcCacheContext.isLogAnnotation()) {
			return;
		}
        
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        //logger.info("前置通知:" + methodName + ", args:" + Arrays.toString(args));
        
        try {
			// 判断是否存在request
			Signature signature = joinPoint.getSignature();
			MethodSignature methodSignature = (MethodSignature)signature;
			Method targetMethod = methodSignature.getMethod();
			//ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
			//String[] parameterNames = parameterNameDiscoverer.getParameterNames(targetMethod);
			//Parameter[] parameters = targetMethod.getParameters();
			Class [] classes = targetMethod.getParameterTypes();
			HttpServletRequest request = null;
			String ipAddr = null;
			
			for (int i = 0; i < classes.length; i++) {
				if ("javax.servlet.http.HttpServletRequest".equals(classes[i].getName())) {
					request = (HttpServletRequest) args[i];
					break;
				}
			}
			
			ipAddr = request != null ? HttpUtil.getIpAddress(request) : NetUtils.getLocalAddress().getHostAddress();
			
			OperateLogTO operateLogTO = new OperateLogTO();
			operateLogTO.setUserCode(DubboFilterUtil.checkUserCodeByAOP(args, logAnnotation, filterClassUrl));
			operateLogTO.setParamsInfo(excludeParam(logAnnotation, args));
			operateLogTO.setFunctionInfo(logAnnotation.function());
			operateLogTO.setIpAddr("0:0:0:0:0:0:0:1".equals(ipAddr)?NetUtils.getLocalAddress().getHostAddress():ipAddr);
			//operateLogTO.setRunInfo(methodName);
			operateLogTO.setRunInfo(DateUtil.getTodayToSecond() + "|" +ipAddr
					+ "|" + joinPoint.getSignature().getDeclaringType().getName()+"."+methodName+"()|"+logAnnotation.function()
					+ "|" +dubboAppName);
			
			// 缓存当前接口上下文信息
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("LOG_"+operateLogTO.getUserCode(), operateLogTO);
			// 缓存当前接口对应的上下文信息
			RpcCacheContext.setServiceInfo(jsonObject.toJSONString());
		} catch (Exception e) {
			logger.error("log4d aop info doing by before is exception：{}", e.getMessage());
		} finally {
			// 有@LogAnnotation
			RpcCacheContext.setLogAnnotation();
		}
    }


	/**
	 * 排除参数
	 */
	private String excludeParam(LogAnnotation logAnnotation, Object[] args) {
		if (logAnnotation.excludeParamNames().length > 0) {
			SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
			filter.getExcludes().addAll(Arrays.asList(logAnnotation.excludeParamNames()));
			SerializeFilter[] filters = {filter};
			return JSON.toJSONString(args, filters);
		}
		return JSON.toJSONString(args);
	}

	// around进行AOP处理
//    @Around("(pointL() or pointR()) && @annotation(logAnnotation))")
//    public Object around(ProceedingJoinPoint joinPoint, LogAnnotation logAnnotation) throws Throwable {
//    	Object proceed = null;
//    	try {
//			//logger.info("around() before proceed");
//
//			String methodName = joinPoint.getSignature().getName();
//			Object[] args = joinPoint.getArgs();
//
//	        
//			proceed = joinPoint.proceed();
//		} catch (Exception e) {
//			logger.error("{}消费端AOP异常：{}", dubboAppName, e.getMessage());
//		}
//        return proceed;
//    }

    /**
     * 后置通知
     * 连接点方法完成后执行
     * 无论连接点执行成功与否该通知都会执行
     */
    @After("point() && @annotation(logAnnotation)")
    public void after(JoinPoint joinPoint, LogAnnotation logAnnotation) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("后置通知:" + methodName + ", args:" + JSON.toJSONString(RpcCacheContext.getTraceContext()));
    	// 清空当前接口缓存信息
        RpcCacheContext.clear();
    }

    /**
     * 连接点方法执行成功后执行
     * 可以拿到返回结果
     */
//    @AfterReturning(value = "point()", returning = "result")
//    public void result(JoinPoint joinPoint, Object result) {
//        String methodName = joinPoint.getSignature().getName();
//        Object[] args = joinPoint.getArgs();
//        System.out.println("AfterReturning() methodName:" + methodName + ", args:" + Arrays.toString(args) + ", result:" + result);
//    }

    /**
     * 连接点方法执行异常后执行
     * 可以拿到异常信息
     */
//    @AfterThrowing(value = "point()", throwing = "exception")
//    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
//        String methodName = joinPoint.getSignature().getName();
//        Object[] args = joinPoint.getArgs();
//        System.out.println("afterThrowing() methodName:" + methodName + ", args:" + Arrays.toString(args) + ", exception:" + exception);
//    }
    
}
