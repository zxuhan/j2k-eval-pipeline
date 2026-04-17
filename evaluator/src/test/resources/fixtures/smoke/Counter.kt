package demo

class Counter {
    private var n = 0

    @kotlin.jvm.Synchronized
    fun next(): Int {
        n++
        return n
    }
}
