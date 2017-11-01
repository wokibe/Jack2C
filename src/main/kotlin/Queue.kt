// copied and simplified from
// https://github.com/gazolla/Kotlin-Algorithm/tree/master/Queue
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

class Queue<T>() {
  private var items: ArrayList<T> = arrayListOf()

  fun isEmpty(): Boolean = this.items.isEmpty()

  fun count(): Int = this.items.count()

  override fun toString() = this.items.toString()

  fun enqueue(element: T) {
    this.items.add(element)
  }

  fun dequeue(): T? {
    if (this.isEmpty()) {
      return null
    } else {
      return this.items.removeAt(0)
    }
  }

  fun peek(): T? {
    return this.items[0]
  }
}
