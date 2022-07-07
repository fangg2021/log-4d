package test.log.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.utils.NetUtils;

public class HttpUtil {

	// \b 是单词边界(连着的两个(字母字符 与 非字母字符) 之间的逻辑上的间隔),
	// 字符串在编译时会被转码一次,所以是 "\\b"
	// \B 是单词内部逻辑间隔(连着的两个字母字符之间的逻辑上的间隔)
	static String phoneReg = "\\b(ip(hone|od)|android|opera m(ob|in)i" + "|windows (phone|ce)|blackberry"
			+ "|s(ymbian|eries60|amsung)|p(laybook|alm|rofile/midp" + "|laystation portable)|nokia|fennec|htc[-_]"
			+ "|mobile|up.browser|[1-4][0-9]{2}x[1-4][0-9]{2})\\b";
	static String tableReg = "\\b(ipad|tablet|(Nexus 7)|up.browser" + "|[1-4][0-9]{2}x[1-4][0-9]{2})\\b";

	// 移动设备正则匹配：手机端、平板
	static Pattern phonePat = Pattern.compile(phoneReg.toLowerCase(), Pattern.CASE_INSENSITIVE);
	static Pattern tablePat = Pattern.compile(tableReg.toLowerCase(), Pattern.CASE_INSENSITIVE);

	/**
	 * 获取Ip地址
	 * @param request
	 * @return
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.isNotEmpty(ip) && ip.startsWith("0:0:0:0:0:0:0:1")) {
            ip = NetUtils.getLocalAddress().getHostAddress();
        }
        if (ip.split(",").length > 1) {
            ip = ip.split(",")[0];
        }
        return ip;
	}

	/** 判断是否入口端类型(0web、1手机、2pad) */
	public static int fromMoblie(HttpServletRequest request) {
		int type = 0;
		if (request.getHeader("User-Agent") != null) {
			String agent = request.getHeader("User-Agent");

			// 匹配
			Matcher matcherPhone = phonePat.matcher(agent.toLowerCase());
			Matcher matcherPad = tablePat.matcher(agent.toLowerCase());
			if (matcherPhone.find()) {
				return 1;
			}
			if (matcherPad.find()) {
				return 2;
			}
		}

		return type;
	}
	
	/**
	 * websocket获取用户IP
	 */
	public static String getWebsocketIp(SocketAddress socketAddress) {
		if (socketAddress instanceof InetSocketAddress) {
			InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();
			if (inetAddress instanceof Inet4Address) {
				// log.info("IPv4:{}",inetAddress);
				return inetAddress.getHostAddress();
			} else if (inetAddress instanceof Inet6Address) {
				// log.info("IPv6:{}",inetAddress);
			} else {
				// log.error("Not an IP address.");
				return null;
			}
		} else {
			// log.error("Not an internet protocol socket.");
		}
		return null;
	}

}
