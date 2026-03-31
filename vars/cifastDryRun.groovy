import com.cifast.*

def call(Map params = [:]) {
    params.dryRun = true
    return cifast(params)
}
