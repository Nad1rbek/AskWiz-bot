package nad1r.techie

fun Boolean.runIfFalse(func: () -> Unit) {
    if (!this) func()
}
fun Boolean.runIfTrue(func: () -> Unit) {
    if (this) func()
}