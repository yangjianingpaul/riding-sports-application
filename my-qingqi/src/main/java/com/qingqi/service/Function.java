package com.qingqi.service;

public interface Function<T, E> {

    T callback(E e);

}