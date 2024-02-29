## 解决公网ip经常变动

### 问题

​	家里申请了电信的公网IP, 但是如果停电或者重新拨号都会导致这个IP变更.个人服务器里很多配置好的应用都不能通过原先IP访问了

### 思考

- 现成的产品类似花生壳这些内网穿透，付费项目不便宜，但是如果使用免费的， 那缺点很明显：

  1. 端口映射有限制，花生壳只能添加两个，且https 需要付费
  2. 带宽有限制

  于是想到了域名配置dns解析, 所有应用和连接都改为域名，那么每次IP 有变动，我只需要改下域名解析的IP 就行。如果每次都需要手动改一下, 那并不是我想要的。

  查了阿里云/腾讯云/华为云等等dns解析都对外提供了api文档, 那么写个定时脚本获取到公网ip再与云上的解析记录做比对,如果云解析记录不是当前的公网ip的话, 更新阿里云的解析记录


### 实现

- Scheduler 定时获取公网IP 去检查更新

  ```java
  import cn.hutool.http.HttpUtil;
  import com.yuanbao.util.AliDnsUtil;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Component;
  
  /**
   * 修改域名定时任务
   *
   * @author leon.yuan
   * @date 2024-02-20
   **/
  @Slf4j
  @Component
  public class DnsDomainNameTask {
  
      @Autowired
      private AliDnsUtil aliDnsUtil;
  
      @Value("${public-ip-address-url}")
      private String ipAddressUrl;
  
      /**
       *  每三分钟获取公网ip去尝试更新解析记录
       *  这里可根据实际自己调节
       */
      @Scheduled(cron = "0 0/3 * * * ? ")
      public void syncIpToDNS(){
          String value = HttpUtil.get(ipAddressUrl).replace("\n", "");
          try {
              aliDnsUtil.updateDnsRecord(value);
          } catch (Exception e) {
              log.error(e.getMessage());
          }
      }
  
  }
  ```

- 尝试更新阿里云解析记录

    ```java
    package com.yuanbao.util;
    
    import com.aliyun.alidns20150109.models.DescribeDomainRecordsRequest;
    import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponse;
    import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody;
    import com.aliyun.alidns20150109.models.UpdateDomainRecordRequest;
    import com.aliyun.teaopenapi.models.Config;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.BeanUtils;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;
    
    /**
     * DNS 解析工具类
     *
     * @author leon.yuan
     * @date 2024-02-20
     **/
    @Slf4j
    @Component
    public class AliDnsUtil {
    
        @Value("${ali.access-key-id}")
        String ACCESS_KEY_ID ;
        @Value("${ali.access-key-secret}")
        String ACCESS_KEY_SECRET;
        @Value("${ali.domain-name}")
        String  DOMAIN_NAME;
        @Value("${ali.end-point}")
        String END_POINT;
    
        /**
         * 使用AK&SK初始化账号Client
         * @param accessKeyId
         * @param accessKeySecret
         * @return Client
         * @throws Exception
         */
        com.aliyun.alidns20150109.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = END_POINT;
            return new com.aliyun.alidns20150109.Client(config);
        }
    
        /**
         *  更新解析记录
         * @param value 当前公网ip
         */
        public  void updateDnsRecord (String value) throws Exception{
            com.aliyun.alidns20150109.Client client = this.createClient(ACCESS_KEY_ID, ACCESS_KEY_SECRET);
    
            DescribeDomainRecordsRequest domainRecordsRequest = new DescribeDomainRecordsRequest();
            domainRecordsRequest.setDomainName(DOMAIN_NAME);
            // 解析记录列表
            DescribeDomainRecordsResponse domainRecordsResponse = client.describeDomainRecords(domainRecordsRequest);
            for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord record : domainRecordsResponse.body.domainRecords.record) {
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                if (record.value.equals(value)) {
                    break;
                }
                BeanUtils.copyProperties(record, updateDomainRecordRequest);
                updateDomainRecordRequest.setValue(value);
                client.updateDomainRecord(updateDomainRecordRequest);
                log.info("域名:{}的主机记录{}更新为: {}",DOMAIN_NAME, record.RR,value);
            }
        }
    }
    
    ```

- 打成jar包运行  
  1. 点击 Maven -> ddns -> package 可以在项目target目录下生成ddns-1.0.jar 
  
  2. 到jar所在的目录下运行程序（linux): nohup java -jar ddns.jar &。
  
  3. 等几分钟进行测试，看看阿里云DNS解析是否被修改。
  
  4. 也可以设置成开机自启，自己网上搜方法， 这里就不过多介绍了。
  
     ```bat
       
     ```

### 测试

去阿里云平台上面把域名的解析记录先改成错误的IP, 过几分钟后会变成了当前的公网ip地址
