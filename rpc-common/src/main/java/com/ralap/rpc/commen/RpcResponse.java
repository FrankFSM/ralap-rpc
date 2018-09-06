package com.ralap.rpc.commen;

import lombok.Data;

/**
 * 封装 RPC 响应
 * 封装相应object
 */
@Data
public class RpcResponse {

    private String requestId;
    private Throwable error;
    private Object result;

    public boolean isError() {
        return error != null;
    }

}
