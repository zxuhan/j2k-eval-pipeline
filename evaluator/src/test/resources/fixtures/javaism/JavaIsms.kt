package fixtures.javaism

import java.util.ArrayList
import java.util.HashMap

class Legacy {
    private var internal: String? = null

    fun getName(): String = internal!!
    fun setName(value: String) { internal = value }

    fun getEnabled(): Boolean = true

    @kotlin.jvm.Synchronized
    fun bump() {
        val list: ArrayList<String> = ArrayList()
        val map: HashMap<String, String> = HashMap()
        if (internal != null) {
            list.add(internal!!)
        }
        for (i in list.indices) {
            println(list[i])
        }
        println(map)
    }
}
