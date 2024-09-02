package com.github.paopaoyue.rpcmod;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;

public interface RpcApi {

    static <T> T getCaller(Class<T> callerClass) {
        assert RpcAutoConfiguration.getProp().isClientEnabled();
        return RpcApp.context.getBean(callerClass);
    }
}
