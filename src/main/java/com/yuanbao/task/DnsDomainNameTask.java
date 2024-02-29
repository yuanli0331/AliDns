package com.yuanbao.task;

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
