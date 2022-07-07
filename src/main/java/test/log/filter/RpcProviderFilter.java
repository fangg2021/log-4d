package test.log.filter;

import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import test.log.entity.RpcCacheContext;

/**
 * PROVIDER拦截器
 * @author fangg
 * 2022年7月7日 上午8:12:15
 */
@Activate(group = CommonConstants.PROVIDER, order = -10000)
public class RpcProviderFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(RpcProviderFilter.class);

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		logger.info("rpc PROVIDER拦截器接口名：{}", invocation.getMethodName());
		logger.info("rpc PROVIDER拦截器接口参数：{}", JSONObject.toJSONString(invocation.getArguments()));
		
		try {
			// 不要把invocation.getObjectAttachments()中的参数直接覆盖到rpcContext.getObjectAttachments()中
			// 只传入自己需要的参数即可，不然会报接口调用失败异常
			Map<String, Object> attachments = invocation.getObjectAttachments();
	        Map<String, Object> newAttach = null;
	        if (attachments != null) {
	            newAttach = new HashMap<>(attachments.size());
	            for (Map.Entry<String, Object> entry : attachments.entrySet()) {
	                String key = entry.getKey();
	                if (key.startsWith("LOG_")) {
	                    newAttach.put(key, entry.getValue());
	                }
	            }
	            
	            if (newAttach.isEmpty()) {
	        		return invoker.invoke(invocation);
				}
	        }
			
			RpcContext.getServiceContext().setInvoker(invoker).setInvocation(invocation);

			RpcContext context = RpcContext.getServerAttachment();
			//        .setAttachments(attachments)  // merged from dubbox
			context.setLocalAddress(invoker.getUrl().getHost(), invoker.getUrl().getPort());

			// mreged from dubbox
			// we may already added some attachments into RpcContext before this
			// filter (e.g. in rest protocol)
			if (newAttach != null && newAttach.isEmpty() == false) {
			    if (context.getObjectAttachments() != null) {
			        context.getObjectAttachments().putAll(newAttach);
			    } else {
			        context.setObjectAttachments(newAttach);
			    }
			    
			    // 防止接口中多次调用接口时数据丢失
			    RpcCacheContext.setRpcInfo(context.getObjectAttachments());
			    //logger.info("rpc PROVIDER拦截器上下文信息：{}", JSONObject.toJSONString(context.getObjectAttachments()));
			}

			if (invocation instanceof RpcInvocation) {
				((RpcInvocation) invocation).setInvoker(invoker);
			}
			try {
				return invoker.invoke(invocation);
			} finally {
				RpcContext.removeServerAttachment();
				RpcContext.removeServerContext();
				RpcContext.removeServiceContext();
			}
		} catch (Exception e) {
			logger.error("DUBBO PROVIDER拦截器异常：", e);
		}
		return invoker.invoke(invocation);
	}

}
