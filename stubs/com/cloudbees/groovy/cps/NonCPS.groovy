package com.cloudbees.groovy.cps

import java.lang.annotation.*

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface NonCPS {}
