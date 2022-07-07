/**
 * 
 */
package test.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在方法上标识表示进行操作日记记录
 * <pre>注解配合dubbot filter/aop使用，默认对注解方法里面的所有接口进行上下文参数信息传递处理，</pre>
 * <pre>如果只想对某个接口进行上下文参数信息传递处理，则在serviceMethod里面加入接口方法名即可。</pre>
 * <pre>(注：只有public方法有效)</pre>
 * @author fangg
 * 2022年7月1日 下午8:58:20
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAnnotation {

	/** 当为true时表示要记录到数据库 */
	boolean write() default false;
	
	/** 功能描述 */
	String function() default "";
	
	/** 需要传递上下文信息的service方法 */
	String [] serviceMethod() default {};
	
	/** 排除参数(保存日记时) */
	String [] excludeParamNames() default {};
	
	/** 指定方法参数中的userCode属性名(对象中的属性名/String参数名) */
	String userCodeFlag() default "userCode";
	
}
