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
