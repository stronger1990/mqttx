package com.jun.mqttx.service.impl;

import com.jun.mqttx.config.MqttxConfig;
import com.jun.mqttx.entity.PubMsg;
import com.jun.mqttx.service.IPublishMessageService;
import com.jun.mqttx.utils.Serializer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * publish message store by redis
 *
 * @author Jun
 * @since 1.0.4
 */
@Service
public class DefaultPublishMessageServiceImpl implements IPublishMessageService {

    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;
    private final Serializer serializer;
    private final String pubMsgSetPrefix;

    public DefaultPublishMessageServiceImpl(ReactiveRedisTemplate<String, byte[]> redisTemplate,
                                            Serializer serializer,
                                            MqttxConfig mqttxConfig) {
        this.redisTemplate = redisTemplate;
        this.serializer = serializer;

        this.pubMsgSetPrefix = mqttxConfig.getRedis().getPubMsgSetPrefix();
        Assert.hasText(pubMsgSetPrefix, "pubMsgSetPrefix can't be null");
    }

    @Override
    public Mono<Void> save(String clientId, PubMsg pubMsg) {
        return redisTemplate.opsForHash()
                .put(pubMsgSetPrefix + clientId, String.valueOf(pubMsg.getMessageId()), serializer.serialize(pubMsg))
                .then();
    }

    @Override
    public Mono<Void> clear(String clientId) {
        return redisTemplate.delete(pubMsgSetPrefix + clientId).then();
    }

    @Override
    public Mono<Void> remove(String clientId, int messageId) {
        return redisTemplate.opsForHash().remove(key(clientId), String.valueOf(messageId)).then();
    }

    @Override
    public Flux<PubMsg> search(String clientId) {
        return redisTemplate.opsForHash().values(key(clientId))
                .map(e -> serializer.deserialize((byte[]) e, PubMsg.class));
    }

    private String key(String client) {
        return pubMsgSetPrefix + client;
    }
}
