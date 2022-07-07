package test.log.entity;

import java.io.Serializable;

/**
 * 操作日记信息TO
 * @author fangg date:2022/07/01 17:32
 */
public class OperateLogTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** 
	 */
	private Long logId;

	/**
	 * 功能
	 */
	private String functionInfo;

	/**
	 * 执行路线信息
	 */
	private String runInfo;

	/**
	 * 入参信息
	 */
	private String paramsInfo;

	/**
	 * 操作人编号
	 */
	private String userCode;

	/**
	 * IP地址
	 */
	private String ipAddr;

	/**
	 * 操作结果信息
	 */
	private String resultInfo;

	/**
	 * 操作时间
	 */
	private Long operateTime;
	private String serviceMethod;	// 接口方法名称
	private Integer inType;			// 操作入口类型，0：WEB端，1：手机端

	public Integer getInType() {
		return inType;
	}

	public void setInType(Integer inType) {
		this.inType = inType;
	}

	public String getServiceMethod() {
		return serviceMethod;
	}

	public void setServiceMethod(String serviceMethod) {
		this.serviceMethod = serviceMethod;
	}

	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
	}

	public String getFunctionInfo() {
		return functionInfo;
	}

	public void setFunctionInfo(String functionInfo) {
		this.functionInfo = functionInfo;
	}

	public String getRunInfo() {
		return runInfo;
	}

	public void setRunInfo(String runInfo) {
		this.runInfo = runInfo;
	}

	public String getParamsInfo() {
		return paramsInfo;
	}

	public void setParamsInfo(String paramsInfo) {
		this.paramsInfo = paramsInfo;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getResultInfo() {
		return resultInfo;
	}

	public void setResultInfo(String resultInfo) {
		this.resultInfo = resultInfo;
	}

	public Long getOperateTime() {
		return operateTime;
	}

	public void setOperateTime(Long operateTime) {
		this.operateTime = operateTime;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" [");
		sb.append("  logId=").append(logId);
		sb.append(", functionInfo=").append(functionInfo);
		sb.append(", runInfo=").append(runInfo);
		sb.append(", paramsInfo=").append(paramsInfo);
		sb.append(", userCode=").append(userCode);
		sb.append(", ipAddr=").append(ipAddr);
		sb.append(", resultInfo=").append(resultInfo);
		sb.append(", operateTime=").append(operateTime);
		sb.append("]");
		return sb.toString();
	}
}