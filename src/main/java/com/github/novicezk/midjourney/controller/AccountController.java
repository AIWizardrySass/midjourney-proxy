package com.github.novicezk.midjourney.controller;

import cn.hutool.core.bean.BeanUtil;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.domain.DiscordAccountWithRuntime;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.ResponseObject;
import com.github.novicezk.midjourney.service.DiscordAccountStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "账号查询")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final DiscordLoadBalancer loadBalancer;

    private final DiscordAccountStoreService discordAccountStoreService;

    @ApiOperation(value = "指定ID获取账号")
    @GetMapping("/{id}/fetch")
    public DiscordAccount fetch(@ApiParam(value = "账号ID") @PathVariable String id) {
        DiscordInstance instance = this.loadBalancer.getDiscordInstance(id);
        return instance == null ? null : instance.account();
    }

    @ApiOperation(value = "查询所有账号")
    @GetMapping("/list")
    public ResponseObject<List<DiscordAccount>> list() {
        return ResponseObject.success(this.loadBalancer.getAllInstances().stream()
                .map(DiscordInstance::account)
                .peek(account -> account.setUserToken("******")).toList());
    }

    @ApiOperation(value = "查询所有账号，以及账号下的运行状态")
    @GetMapping("/listWithRuntime")
    public ResponseObject<List<DiscordAccountWithRuntime>> listWithRuntime() {
        return ResponseObject.success(this.loadBalancer.getAllInstances().stream().map(instance -> {
            DiscordAccountWithRuntime account = new DiscordAccountWithRuntime();
            BeanUtil.copyProperties(instance.account(), account);
            account.setUserToken("******");
            account.setWaitingQueueSize(instance.getRunningFutures().size());
            account.setRunningTaskSize(instance.getRunningTasks().size());
            return account;
        }).toList());
    }

    @ApiOperation(value = "增加账号")
    @PostMapping("/addAccount")
    public ResponseObject<Void> addAccount(@RequestBody DiscordAccount account) {
        if (Strings.isEmpty(account.getId())) {
            account.setId(account.getChannelId());
        }
        DiscordInstance discordInstance = loadBalancer.getDiscordInstance(account.getGuildId());
        if (discordInstance != null && discordInstance.isAlive()) {
            // 这个状态下只允许修改tag标签
            if (Strings.isNotBlank(account.getTag())) {
                discordInstance.account().setTag(account.getTag());
            }
            discordAccountStoreService.saveAccount(account);
            return ResponseObject.success();
        }

        loadBalancer.removeAccount(account.getId());
        boolean res = loadBalancer.addAccount(account);

        if (res) {
            discordAccountStoreService.saveAccount(account);
        }

        return res ? ResponseObject.success() : ResponseObject.innerError("添加失败");
    }

    @ApiOperation(value = "移除指定账号")
    @DeleteMapping("/{id}/delete")
    public ResponseObject<Void> deleteAccount(@ApiParam(value = "账号ID") @PathVariable String id) {
        DiscordInstance discordInstance = loadBalancer.getDiscordInstance(id);
        if (discordInstance == null) {
            return ResponseObject.paramInvalid("账户ID不正确");
        }

        if (discordInstance.isAlive()) {
            return ResponseObject.paramInvalid("账号正在使用中不可移除");
        }
        loadBalancer.removeAccount(id);
        discordAccountStoreService.removeAccount(id);
        return ResponseObject.success();
    }

    @ApiOperation(value = "重新链接")
    @PostMapping("/{id}/retryConnect")
    public ResponseObject<Void> retryConnect(@ApiParam(value = "账号ID") @PathVariable String id) {
        DiscordInstance discordInstance = loadBalancer.getDiscordInstance(id);
        if (discordInstance == null) {
            return ResponseObject.paramInvalid("账户ID不正确");
        }

        if (discordInstance.isAlive()) {
            return ResponseObject.paramInvalid("账号已经是连接状态");
        }

        boolean isSuccess = loadBalancer.retryConnect(id);

        return isSuccess ? ResponseObject.success() : ResponseObject.innerError("连接失败");
    }
}