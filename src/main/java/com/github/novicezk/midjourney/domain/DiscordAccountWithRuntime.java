package com.github.novicezk.midjourney.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yubo.ge
 * @date 2023/9/5 22:53
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("Discord账号包含运行数据")
public class DiscordAccountWithRuntime extends DiscordAccount {

    @ApiModelProperty("当前队列排队中数两")
    private Integer waitingQueueSize;


    @ApiModelProperty("当前运行中的任务数")
    private Integer runningTaskSize;
}
