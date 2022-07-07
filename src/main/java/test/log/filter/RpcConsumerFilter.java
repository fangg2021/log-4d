package test.log.filter;

import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.alibaba.fastjson.JSONObject;

import test.log.config.ApplicationContextHolder;
import test.log.entity.RpcCacheContext;
import test.log.util.DubboFilterUtil;

/**
 * CONSUMER拦截器
 * @author fangg
 * 2022年7月2日 下午12:00:33
 */
@Activate(group = CommonConstants.CONSUMER, order = -10000)
public class RpcConsumerFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(RpcConsumerFilter.class);
	private static String FILTER_CLASS_URL;
	private static String DUBBO_APPLICATION;
//	RedisDbTemplate redisDbTemplate = null;
	Environment environment = null;
	

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		logger.info("CONSUMER拦截器接口名：{}", invocation.getMethodName());
		logger.info("CONSUMER拦截器接口参数：{}", JSONObject.toJSONString(invocation.getArguments()));
		
		try {
			// 判断当前主接口是否有@LogAnnotation
			if (RpcCacheContext.isLogAnnotation() == false) {
				return invoker.invoke(invocation);
			}
			
			// 再读取配置文件中的变量
			if (environment == null) {
				environment = ApplicationContextHolder.getBean(Environment.class);
				FILTER_CLASS_URL = environment.getProperty("log4d.filter.url");
				DUBBO_APPLICATION = environment.getProperty("dubbo.application.name");
			}
			
			// 过滤没有@LogAnnotation注解的方法
			Map<String, Object> paramsMap = new HashMap<>();
			if (DubboFilterUtil.checkLogAnnotation(FILTER_CLASS_URL, invocation.getMethodName(), paramsMap) == false) {
				return invoker.invoke(invocation);
			}

			RpcContext rpcContext = RpcContext.getServerAttachment();
			// rpcContext上下文参数处理
			DubboFilterUtil.checkRpcContext(invocation.getArguments(), paramsMap, rpcContext.getObjectAttachments(), 
					NetUtils.getLocalAddress().getHostAddress(), DUBBO_APPLICATION);
			
			invocation.getObjectAttachments().putAll(rpcContext.getObjectAttachments());
			//rpcContext.getObjectAttachments().putAll(invocation.getObjectAttachments());
        	//rpcContext.setInvoker(invoker).setInvocation(inv);
			//logger.info("{}接口方法需要日记记录:ticket={}", invocation.getMethodName(), request.getAttribute("ticket"));

	        try {
				// 在当前的RpcContext中记录本地调用的一次状态信息
				rpcContext
		                .setInvoker(invoker)
		                .setInvocation(invocation)
		                .setLocalAddress(NetUtils.getLocalHost(), 0)
		                .setRemoteAddress(invoker.getUrl().getHost(),
		                        invoker.getUrl().getPort());
		        if (invocation instanceof RpcInvocation) {
		            ((RpcInvocation) invocation).setInvoker(invoker);
		        }
	            return invoker.invoke(invocation);
	        } finally {
	        	rpcContext.clearAttachments();
	        }
		} catch (Exception e) {
			logger.error("DUBBO CONSUMER {}拦截器判断异常：", DUBBO_APPLICATION, e);
		}
		return invoker.invoke(invocation);
	}

}
