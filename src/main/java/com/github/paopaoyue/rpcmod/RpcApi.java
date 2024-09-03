package com.github.paopaoyue.rpcmod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;

public interface RpcApi {

    Logger logger = LogManager.getLogger(RpcApi.class);

    static <T> T getCaller(Class<T> callerClass) {
        try {
            return RpcApp.context.getBean(callerClass);
        } catch (BeansException e) {
            throw new IllegalStateException("Caller not found: " + callerClass.getName() + ". the class may not be in the component scan base package('com.github')!", e);
        }
    }
}
