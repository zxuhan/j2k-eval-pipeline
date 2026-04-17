package fixtures.idiom

data class User(val name: String, val age: Int)

object Config {
    const val FLAG = 1
}

class Thing {
    companion object {
        fun create() = Thing()
    }

    val display: String
        get() = "Thing"
}

fun demo(users: List<User>) {
    val greeting = "Hello, ${users.first().name}!"

    val transformed = users.map { it.name.uppercase() }

    users.forEach {
        println(it.age)
    }

    val grade = when (users.size) {
        0 -> "none"
        1 -> "one"
        else -> "many"
    }

    val runnable = object : Runnable {
        override fun run() {
            println(grade)
        }
    }

    val result = users.firstOrNull()?.let { u ->
        u.name.takeIf { it.isNotBlank() }
    }

    if (users.isEmpty()) {
        println("empty")
    } else if (users.size == 1) {
        println("single")
    } else {
        println("many")
    }

    users.firstOrNull()?.apply {
        println(name)
    }

    runnable.run()
    println(transformed)
    println(result)
}
