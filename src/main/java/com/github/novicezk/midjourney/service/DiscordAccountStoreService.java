package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.domain.DiscordAccount;

import java.util.List;

/**
 * discord账号管理
 * @author xmly
 */
public interface DiscordAccountStoreService {

    /**
     * 存discord账号信息, 对于账号信息有变化，调用此方法进行持久化
     * @param account 账号信息
     */
     void saveAccount(DiscordAccount account);
    /**
     * 删除账号
     * @param accountId 账号ID
     */
    void removeAccount(String accountId);

    /**
     * 获取所有的账号信息
     * @return
     */
    List<DiscordAccount> getAllAccount();

}
