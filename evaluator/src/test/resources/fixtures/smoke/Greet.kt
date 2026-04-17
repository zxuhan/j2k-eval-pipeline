package demo

interface Greet {
    fun hello(who: String?): String? {
        return "hi " + who
    }
}
