/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.dromara.soul.admin.listener;

import org.apache.commons.collections4.CollectionUtils;
import org.dromara.soul.admin.service.AppAuthService;
import org.dromara.soul.admin.service.PluginService;
import org.dromara.soul.admin.service.RuleService;
import org.dromara.soul.admin.service.SelectorService;
import org.dromara.soul.common.dto.AppAuthData;
import org.dromara.soul.common.dto.ConfigData;
import org.dromara.soul.common.dto.PluginData;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.enums.ConfigGroupEnum;
import org.dromara.soul.common.utils.GsonUtils;
import org.dromara.soul.common.utils.MD5Utils;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract class for ConfigEventListener.
 * As we think that the md5 value of the in-memory data is the same as the md5 value of the database,
 * although it may be a little different, but it doesn't matter, we will have thread to periodically
 * pull the data in the database.
 * @author huangxiaofeng
 * @date 2019/6/25 17:58
 * @since 2.0.0
 */
public abstract class AbstractDataChangedListener implements DataChangedListener, InitializingBean {

    @Resource
    protected AppAuthService appAuthService;

    @Resource
    protected PluginService pluginService;

    @Resource
    protected RuleService ruleService;

    @Resource
    protected SelectorService selectorService;

    private static final Logger logger = getLogger(AbstractDataChangedListener.class);

    /**
     * groupKey -> cacheItem
     */
    protected static final ConcurrentHashMap<String, ConfigDataCache> CACHE = new ConcurrentHashMap<>();

    /**
     * fetch config from database.
     */
    public ConfigData<?> fetchConfig(ConfigGroupEnum groupKey) {
        ConfigDataCache config = CACHE.get(groupKey.name());
        switch (groupKey) {
            case APP_AUTH:
                return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), appAuthService.listAll());
            case PLUGIN:
                return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), pluginService.listAll());
            case RULE:
                return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), ruleService.listAll());
            case SELECTOR:
                return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), selectorService.listAll());
            default:
                throw new IllegalStateException("Unexpected groupKey: " + groupKey);
        }
    }

    @Override
    public void onAppAuthChanged(List<AppAuthData> changed, DataEventType eventType){
        if (CollectionUtils.isEmpty(changed)) {
            return;
        }
        this.updateAppAuthCache();
        this.afterAppAuthChanged(changed, eventType);
    }
    
    protected abstract void afterAppAuthChanged(List<AppAuthData> changed, DataEventType eventType);

    @Override
    public void onPluginChanged(List<PluginData> changed, DataEventType eventType){
        if (CollectionUtils.isEmpty(changed)) {
            return;
        }
        this.updatePluginCache();
        this.afterPluginChanged(changed, eventType);
    }

    protected abstract void afterPluginChanged(List<PluginData> changed, DataEventType eventType);

    @Override
    public void onRuleChanged(List<RuleData> changed, DataEventType eventType){
        if (CollectionUtils.isEmpty(changed)) {
            return;
        }
        this.updateRuleCache();
        this.afterRuleChanged(changed, eventType);
    }

    protected abstract void afterRuleChanged(List<RuleData> changed, DataEventType eventType);

    @Override
    public void onSelectorChanged(List<SelectorData> changed, DataEventType eventType){
        if (CollectionUtils.isEmpty(changed)) {
            return;
        }
        this.updateSelectorCache();
        this.afterSelectorChanged(changed, eventType);
    }

    protected abstract void afterSelectorChanged(List<SelectorData> changed, DataEventType eventType);

    /**
     * init cache
     * @throws Exception
     */
    @Override
    public final void afterPropertiesSet() {
        updateAppAuthCache();
        updatePluginCache();
        updateRuleCache();
        updateSelectorCache();
    }

    protected void updateSelectorCache() {
        try {
            String json = GsonUtils.getInstance().toJson( selectorService.listAll() );
            String group = ConfigGroupEnum.SELECTOR.name();
            CACHE.put(group, new ConfigDataCache(group, MD5Utils.md5(json), System.currentTimeMillis()));
        } catch (Exception e) {
            logger.warn("updateSelectorCache error.", e);
        }
    }

    protected void updateRuleCache() {
        try {
            String json = GsonUtils.getInstance().toJson( ruleService.listAll() );
            String group = ConfigGroupEnum.RULE.name();
            CACHE.put(group, new ConfigDataCache(group, MD5Utils.md5(json), System.currentTimeMillis()));
        } catch (Exception e) {
            logger.warn("updateRuleCache error.", e);
        }
    }

    protected void updatePluginCache() {
        try {
            String json = GsonUtils.getInstance().toJson( pluginService.listAll() );
            String group = ConfigGroupEnum.PLUGIN.name();
            CACHE.put(group, new ConfigDataCache(group, MD5Utils.md5(json), System.currentTimeMillis()));
        } catch (Exception e) {
            logger.warn("updatePluginCache error.", e);
        }
    }

    protected void updateAppAuthCache() {
        try {
            String json = GsonUtils.getInstance().toJson( appAuthService.listAll() );
            String group = ConfigGroupEnum.APP_AUTH.name();
            CACHE.put(group, new ConfigDataCache(group, MD5Utils.md5(json), System.currentTimeMillis()));
        } catch (Exception e) {
            logger.warn("updateAppAuthCache error.", e);
        }
    }

}