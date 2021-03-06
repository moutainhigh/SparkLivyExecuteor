package com.github.superzhc.livy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import com.github.superzhc.spark.AbstractSparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2020年06月01日 superz add
 */
public class SparkLivyProxy<T extends AbstractSparkSession> implements InvocationHandler
{
    private static final Logger logger = LoggerFactory.getLogger(SparkLivyProxy.class);

    private SparkLivyClient client;
    private T target;

    public SparkLivyProxy(SparkLivyClient client, T target) {
        this.client = client;
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        Class clazz = target.getClass();

        // 参考mybatis的对方法的拦截
        if (Object.class.equals(method.getDeclaringClass()) || //
                (null != method.getAnnotation(SparkLivyLocal.class)
                        && method.getAnnotation(SparkLivyLocal.class).value() == true)) {
            logger.debug("本地调用：[{}]，执行方法：{}，参数：{}", target.getClass().getSimpleName(), method.getName(),
                    Arrays.toString(params));
            return method.invoke(target, params);
        }

        logger.debug("通过Livy调用：[{}]，执行方法：[{}]，参数：{}", clazz.getSimpleName(), method.getName(), Arrays.toString(params));
        /* 2020年6月9日 代理调用的方法只是将方法传递给Livy上执行同样的方法，所以可以直接将方法的参数类型直接传递给Livy上进行反射的方法 */
        Object obj = client.submit(new SparkLivyJob(target, method.getName(), method.getParameterTypes(), params));
        if (null!=obj&&obj.getClass() == clazz) {// 若返回值的类型跟被代理的类型式一样的，要使返回值也被代理起来
            return newProxyInstance(client, (T) obj);
        }
        return obj;
    }

    public static <T extends AbstractSparkSession> Object newProxyInstance(SparkLivyClient client, T obj) {
        SparkLivyProxy<T> proxy = new SparkLivyProxy(client, obj);
        Class clazz = obj.getClass();
        return Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), proxy);
    }
}
