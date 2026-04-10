package com.cifast

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput

class Metrics implements Serializable {
    def steps

    Metrics(def steps) {
        this.steps = steps
    }

    void emit(Map event) {
        def json = formatEvent(event)
        steps.echo("[ci-fast-metric] ${json}")
    }

    @NonCPS
    static String formatEvent(Map event) {
        event.timestamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return JsonOutput.toJson(event)
    }
}
