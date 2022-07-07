# log-4d
需要在dubbo之间传递上下文信息（主要是处理log）

对Dubbo3.0以上版本有效

引入此包需要配置的信息：
1、pom.xml文件中配置
	<build>
		...
		
		<resources>
			<resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/*.*</include>
                </includes>
            </resource>
        </resources>
	</build>

2、application.properties文件中配置
	# dubbo应用名称
	dubbo.application.name=xxx
	# dubbo拦截类路径(只支持单个路径前缀匹配)
	log4d.filter.url=com.test.log.service.impl
	

3、XXXApplication.java启动类中添加
	@ComponentScan({"test.log.config"})


如果哪个接口/方法需要记录（传递）上下文操作信息时，加上注解@LogAnnotation()，然后就可以通过RpcCacheContext（线程安全）获取dubbo服务之间传递的上下文信息了
