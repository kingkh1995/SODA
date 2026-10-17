package com.soda.support.architecture;

import org.springframework.modulith.core.ApplicationModules;

/** 输出完整 SODA Modulith 模块结构，供人工诊断使用。 */
public final class SodaModulithDiagnostics {

    private SodaModulithDiagnostics() {}

    /**
     * 打印完整模块结构。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        ApplicationModules.of(SodaModulith.class).forEach(System.out::println);
    }
}
