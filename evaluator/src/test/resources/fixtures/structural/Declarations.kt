package fixtures.structural

// Top-level comment

class TopClass {
    val name: String = ""
    var age: Int = 0

    fun greet(): String = "hi"

    class Inner {
        fun inside() {
            run {
                // nested block
                val x = 1
            }
        }
    }
}

object Singleton {
    fun ping() {}
}

interface Greeter {
    fun hello()
}
