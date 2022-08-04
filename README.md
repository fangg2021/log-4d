
需要在dubbo之间传递上下文信息（主要是处理log）

对Dubbo3.0以上版本有效

引入此包需要配置的信息：

	1、pom.xml文件中配置:

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

	2、application.properties文件中配置:

		# dubbo应用名称
		dubbo.application.name=xxx
		# dubbo拦截类路径(只支持单个路径前缀匹配)
		log4d.filter.url=com.test.log.service.impl


	3、XXXApplication.java启动类中添加:

		@ComponentScan({"test.log.config"})


如果哪个接口/方法需要记录（传递）上下文操作信息时，加上注解@LogAnnotation()，然后就可以通过RpcCacheContext（线程安全）获取dubbo服务之间传递的上下文信息了

传递的LOG信息如下：

{"LOG_A6d424268":"{"function":"用户登录","paramsInfo":"{"fingerPrint":"test","loginIdentifier":"123456","userCode":"A6d424268","userType":1}","runInfo":"2022-07-17 10:46:25|192.168.31.131|com.test.service.impl.system.SysUserServiceImpl.userLogin()|用户登录|dubbo-test-provider","userCode":"A6d424268"}","input":"984","remote.application":"dubbo-test-consumer"}

效果如(不在此项目中)：

![log](https://user-images.githubusercontent.com/80429579/182769131-f2e1d5d3-e826-41fe-bb8d-357a75453e79.JPG)
