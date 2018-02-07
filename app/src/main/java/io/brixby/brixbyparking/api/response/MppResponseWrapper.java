package io.brixby.parking.api.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class MppResponseWrapper<T extends MppResponse> implements ParameterizedType {

    private Class<?> wrapped;

    public MppResponseWrapper(Class<?> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[]{wrapped};
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public Type getRawType() {
        return MppResponseWrapper.class;
    }

    private T response;
    private int status;

    public T getResponse() {
        return response;
    }

    public boolean isOK() {
        return status == 1;
    }

    public MppResponseWrapper<T> updateStatus() {
        response.setOk(isOK());
        return this;
    }
}
