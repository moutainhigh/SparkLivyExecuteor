package com.github.superzhc;

import com.github.superzhc.common.SparkSQL;
import com.github.superzhc.common.impl.SparkSQLImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.superzhc.dataframe.SparkDataFrame;
import com.github.superzhc.dataframe.SparkDataFrameImpl;
import com.github.superzhc.livy.SparkLivyClient;
import com.github.superzhc.livy.SparkLivyProxy;

import java.util.Properties;

/**
 * 2020年06月01日 superz add
 */
public class SparkDao
{
    private final static Logger logger = LoggerFactory.getLogger(SparkDao.class);

    private String url;
    private SparkLivyClient client;
    private Integer id;

    public SparkDao(String url) {
        this.url = url;
        client = new SparkLivyClient();
        this.id = client.getSessionId();
        logger.info("SessionId:{}", id);
    }

    public SparkDao(String url, Integer id) {
        this.url = url;
        this.id = id;
        client = new SparkLivyClient(id);
    }

    public SparkDataFrame query(String sql, String alias) {
        SparkSQL sparkSQL1 = (SparkSQL) SparkLivyProxy.newProxyInstance(client, new SparkSQLImpl());
        // logger.debug("数据库[{}]执行语句：{}", url, sql);
        String dfKey;
        if (null == url || url == "" || url.startsWith("jdbc:hive2")) {
            dfKey = sparkSQL1.hive(sql);
        }
        else {
            // BUG：在服务器执行可能会报异常：java.sql.SQLException:No suitable driver
            // 2020年6月10日 添加如下属性，数据库连接驱动
            Properties props = new Properties();
            if (url.startsWith("jdbc:oracle:thin")) {
                props.put("driver", "oracle.jdbc.driver.OracleDriver");
            }
            else if (url.startsWith("jdbc:mysql")) {
                props.put("driver", "com.mysql.jdbc.Driver");
            }
            dfKey = sparkSQL1.jdbc(url, "(" + sql + ") " + alias, props);
        }
        logger.debug("DataFrame的唯一标识：{}", dfKey);
        SparkDataFrameImpl sparkDataFrame = new SparkDataFrameImpl(dfKey, alias);
        return (SparkDataFrame) SparkLivyProxy.newProxyInstance(client, sparkDataFrame);
    }

    /**
     * 获取Livy的SessionId
     * @return
     */
    public Integer getId() {
        return id;
    }
}
